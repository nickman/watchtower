import com.ibm.mq.constants.MQConstants
import static com.ibm.mq.constants.MQConstants.*
import java.text.SimpleDateFormat
import com.ibm.mq.pcf.*
import static com.ibm.mq.pcf.CMQC.*
import javax.management.*;
import java.util.concurrent.*;


def pcf = null;
def qManager = null;
def tsdbSocket = null;
def buff = new StringBuilder();
def dupd = new HashSet<String>();
def HOST = InetAddress.getLocalHost().getHostName();
def byte[] emptyConn = new byte[24];
def matchingQueues = new HashSet();
def queueMaxDepths = [:];
def channelNames = null;
def cachePrefix = "stats.$qManager/";
def channelNamesCacheKey = cachePrefix + "ChannelNames";
def queueNamesCacheKey = cachePrefix + "QueueNames";
def topicNamesCacheKey = cachePrefix + "TopicNames";
def queueMaxDepthCacheKey = cachePrefix + "MaxQDepths";
def cache = new ConcurrentHashMap();
def cacheInvalidator = Thread.startDaemon("CacheInvalidator") {
  try {
      Thread.currentThread().join(120000);
      cache.clear();
  } catch (InterruptedException iex) {
      println "Stopped Cache Expiration Thread";
  }
}



public String getHexString(byte[] b) throws Exception {
      String result = "";
      for (int i=0; i < b.length; i++) {
        result +=
              Integer.toString( ( b[i] & 0xff ) + 0x100, 16).substring( 1 );
      }
      return result;
}



public List request(byName, agent, type, parameters) {
    def responses = [];
    def PCFMessage request = new PCFMessage(type);
    if(parameters.getClass().isArray()) {
        parameters.each() { param ->
            request.addParameter(param);
        }
    } else {
        parameters.each() { name, value ->
            request.addParameter(name, value);
        }
    }

    agent.send(request).each() {
        def responseValues = [:];
        it.getParameters().toList().each() { pcfParam ->
            def value = pcfParam.getValue();
            if(value instanceof String) value = value.trim();
            responseValues.put(byName ? pcfParam.getParameterName() : pcfParam.getParameter(), value);
        }
        responses.add(responseValues);
    }
    return responses;
}

stime = {
    return (long)System.currentTimeMillis()/1000;
}

flush = {
    //println buff;
    tsdbSocket << buff;
    buff.setLength(0);
    dupd.clear();
}

on = { name ->
    return new ObjectName(name);
}

pflush = {
    print buff;
    buff.setLength(0);
    dupd.clear();
}


trace = { metric, value, tags, moretags ->
    sb = new StringBuilder();
    now = (long)System.currentTimeMillis()/1000;
    sb.append(now).append("|").append(metric);
    buff.append("put $metric $now $value host=$HOST ");
    tags.each() { k, v ->
        sb.append("|").append(k).append("=").append(v);
        buff.append(k).append("=").append(v).append(" ");
    }
    if(moretags!=null) {
        moretags.each() { k, v ->
            sb.append("|").append(k).append("=").append(v);
            buff.append(k).append("=").append(v).append(" ");
        }
    }
    key = sb.toString();
    if(dupd.contains(key)) {
        System.err.println("DUP: ${key}");
    } else {
        dupd.add(key);
    }
    buff.append("\n");
}

ctrace = { metric, value, tags ->
    if(value!=-1) {
        trace(metric, value, tags);
    }
}

public long time(Closure cl) {
    long start = System.currentTimeMillis();
    cl.call();
    return System.currentTimeMillis()-start;
}

public void time(cl, metricName, namespace) {
    time(cl, metricName, namespace, null);    
}

public void time(cl, metricName, namespace, namespace2) {
    long start = System.currentTimeMillis();
    cl();
    trace(metricName, System.currentTimeMillis() - start, namespace, namespace2);
}

public String resolveChType(int type){
    Map channelTypes = [:]
    channelTypes.put(1, "Sender");
    channelTypes.put(2, "Server");
    channelTypes.put(3, "Receiver");
    channelTypes.put(4, "Requester");
    channelTypes.put(6, "Client Connection");
    channelTypes.put(7, "Server Connection");
    channelTypes.put(8, "Cluster Receiver");
    channelTypes.put(9, "Cluster Sender");
    return channelTypes.get(type);
}


public int perc(part, total) {
    if(part<1 || total <1) return 0;
    return part/total*100;
}

cachePut = { key, value ->
  cache.put(key, value);
}
cacheGet = { key ->
  return cache.get(key);
}



//trace = { metric, value, tags ->

doQueueStats = {
    // =================================================
    // Get Queue Metrics
    // =================================================
    time({
        request(false, pcf, CMQCFC.MQCMD_INQUIRE_Q_STATUS, [(CMQC.MQCA_Q_NAME):"*", (CMQC.MQIA_Q_TYPE):CMQC.MQQT_LOCAL]).each() {
            def q = it.get(CMQC.MQCA_Q_NAME).trim();
            def namespace = ['qmanager' : qManager, 'name':q];
            trace("mq.queue", it.get(CMQC.MQIA_CURRENT_Q_DEPTH), namespace, ['stat':'depth']);
            trace("mq.queue", it.get(CMQC.MQIA_OPEN_INPUT_COUNT), namespace, ['stat':'openinput']);
            trace("mq.queue", it.get(CMQC.MQIA_OPEN_OUTPUT_COUNT), namespace, ['stat':'openoutput']);
            trace("mq.queue", it.get(CMQCFC.MQIACF_UNCOMMITTED_MSGS), namespace, ['stat':'uncommited']);
            trace("mq.queue", it.get(CMQCFC.MQIACF_OLDEST_MSG_AGE), namespace, ['stat':'ageoom']);
            try {
                if("PTMS.TO.ECS.METAQUEUE"==q) {
                    //println "Q: $q, TIND: ${it.get(1226)}";            
                }
            } catch (e) { println e; }
            
        }
    }, "mq.monitor", ["qmanager":qManager, 'stat':'elapsed', 'monitor':'queuemetrics']);
}


doGetTopicNames = {
  // =================================================
  // Get Topic Names
  // =================================================
  time({
    if(cacheGet(topicNamesCacheKey) == null) {
      topicNames = request(false, pcf, CMQCFC.MQCMD_INQUIRE_TOPIC_NAMES, [(CMQC.MQCA_TOPIC_NAME):"*"]).get(0).get(CMQCFC.MQCACF_TOPIC_NAMES);
      for(i in 0..topicNames.length-1) {
        topicNames[i] = topicNames[i].trim();
      }
      cachePut(topicNamesCacheKey, topicNames);
      trace("mq.destinations", topicNames.length, ['qmanager' : qManager, 'stat':'topiccount']);
    }
  }, "mq.monitor", ["qmanager":qManager, 'stat':'elapsed', 'monitor':'topicnames']);

}


doGetQueueNames = {
    // =================================================
    // Get Queue Names
    // =================================================
    time({
        if(cacheGet(queueNamesCacheKey) == null) {
            queueNames = request(false, pcf, CMQCFC.MQCMD_INQUIRE_Q_NAMES, [(CMQC.MQCA_Q_NAME):"*", (CMQC.MQIA_Q_TYPE):CMQC.MQQT_LOCAL]).get(0).get(CMQCFC.MQCACF_Q_NAMES);
            for(i in 0..queueNames.length-1) {
                queueNames[i] = queueNames[i].trim();
                if(!queueNames[i].startsWith("SYSTEM.") && !queueNames[i].startsWith("AMQ.")) {
                    matchingQueues.add(queueNames[i]);
                }
            }
            cachePut(queueNamesCacheKey, queueNames);
        }
      }, "mq.monitor", ["qmanager":qManager, 'stat':'elapsed', 'monitor':'queuenames']);
}

doGetQueueMaxDepths = {
    // =================================================
    // Get Max Queue Depths
    // =================================================
     time({
        element = cache.get(queueMaxDepthCacheKey);
        if(cacheGet(queueMaxDepthCacheKey) == null) {
            queueMaxDepths.clear();
            request(false, pcf, CMQCFC.MQCMD_INQUIRE_Q, [(CMQC.MQCA_Q_NAME):"*", (CMQC.MQIA_Q_TYPE):CMQC.MQQT_LOCAL, (CMQCFC.MQIACF_Q_ATTRS):[CMQC.MQCA_Q_NAME, CMQC.MQIA_MAX_Q_DEPTH] as int[]]).each() {
                queueMaxDepths.put(it.get(CMQC.MQCA_Q_NAME).trim(), it.get(CMQC.MQIA_MAX_Q_DEPTH));
            }
            cachePut(queueMaxDepthCacheKey, queueMaxDepths);
        }
      }, "mq.monitor", ["qmanager":qManager, 'stat':'elapsed', 'monitor':'maxqueuedepths']);
}


doChannelStats = {
    // =================================================
    // Get Channel Stats
    // =================================================
    time({
        request(false, pcf, MQConstants.MQCMD_INQUIRE_CHANNEL_STATUS, [(MQConstants.MQCACH_CHANNEL_NAME):"*"]).each() {
            channelName = it.get(MQConstants.MQCACH_CHANNEL_NAME).trim();            
            channelType = resolveChType(it.get(MQConstants.MQIACH_CHANNEL_TYPE));
            connectionName = it.get(MQConstants.MQCACH_CONNECTION_NAME);
            xmitQueueName = it.get(MQConstants.MQCACH_XMIT_Q_NAME);
            disposition = it.get(MQConstants.MQIACH_CHANNEL_DISP);
            instanceType = it.get(MQConstants.MQIACH_CHANNEL_INSTANCE_TYPE);
            println "Channel:\n\tName: $channelName\n\tConnection Name: $connectionName\n\tXMIT Queue: $xmitQueueName\n\tChannel Type: $channelType\n\tDisposition: $disposition\n\tInstance Type: $instanceType";
            def namespace = ['qmanager' : qManager, 'name': channelName, 'type':channelType];
            trace("mq.channel", it.get(MQConstants.MQIACH_BUFFERS_RCVD), namespace, ['unit':'buffers', 'dir':'received']); //"Buffers Received", "WebSphere MQ", qManager, "Channels", channelType, channelName);
            trace("mq.channel", it.get(MQConstants.MQIACH_BUFFERS_SENT), namespace, ['unit':'buffers', 'dir':'sent']); // , "Buffers Sent", "WebSphere MQ", qManager, "Channels", channelType, channelName);
            trace("mq.channel", it.get(MQConstants.MQIACH_BYTES_RCVD), namespace, ['unit':'bytes', 'dir':'received']);  // , "Bytes Received", "WebSphere MQ", qManager, "Channels", channelType, channelName);
            trace("mq.channel", it.get(MQConstants.MQIACH_BYTES_SENT), namespace, ['unit':'bytes', 'dir':'sent']);  // "Bytes Sent", "WebSphere MQ", qManager, "Channels", channelType, channelName);
        }
      }, "mq.monitor", ["qmanager":qManager, 'stat':'elapsed', 'monitor':'channelstats']);
}

try {
    tsdbSocket = new Socket("opentsdb", 4242);
    println "TSDB Connected";
    pcf = new PCFMessageAgent("localhost", 1415, "JBOSS.SVRCONN")
    println "PCF Connected";
    qManager = pcf.getQManagerName();
    println "QManager: $qManager";
    while(true) {
        long start = System.currentTimeMillis();
        //  ===============================================================================
        //  Start Monitoring
        //  ===============================================================================
        try {
            doGetQueueNames();
            doGetQueueMaxDepths();
            doQueueStats();
            //doChannelStats();
        } finally {
            flush();
            //break;
        }
    
        //  ===============================================================================
        //  End Monitoring
        //  ===============================================================================
        long elapsed = System.currentTimeMillis() - start;
        println "Collected in $elapsed ms.";
        try {
            Thread.sleep(15000);
        } catch (InterruptedException iex) {
            println "Monitor Stopped";
            break;
        }

    } 

} finally {
    try { tsdbSocket.close(); } catch (e) {}
    try { pcf.disconnect(); } catch (e) {}
    try { cacheInvalidator.interrupt(); } catch (e) {}
    cacheInvalidator = null;
}
