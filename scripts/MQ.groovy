import com.ibm.mq.constants.MQConstants
import static com.ibm.mq.constants.MQConstants.*
import java.text.SimpleDateFormat
import java.util.regex.*;
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
    now = Math.round(System.currentTimeMillis()/1000);
    sb.append(now).append("|").append(metric);
    buff.append("put $metric $now $value host=$HOST ");
    tags.each() { k, v ->
        sb.append("|").append(k.replace(" ", "")).append("=").append(v.replace(" ", ""));
        buff.append(k.replace(" ", "")).append("=").append(v.replace(" ", "")).append(" ");
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

SKIP_QUEUE = Pattern.compile("SYSTEM\\..*||AMQ\\..*");
skipQ = { name ->
    return SKIP_QUEUE.matcher(name).matches();
}

//trace = { metric, value, tags ->

doQueueStats = { name ->
    // =================================================
    // Get Queue Metrics
    // =================================================
    if(name==null || name.trim.isEmpty()) {
        name = "*";
    }
    queueNames = new HashSet();
    time({
        request(false, pcf, CMQCFC.MQCMD_INQUIRE_Q_STATUS, [(CMQC.MQCA_Q_NAME):name, (CMQC.MQIA_Q_TYPE):CMQC.MQQT_LOCAL]).each() {
            def q = it.get(CMQC.MQCA_Q_NAME).trim();
            if("*".equals(name) && skipQ(q)) {
                //println "SKIP: $q";
            } else {
                queueNames.add(q);
                def namespace = ['qmanager' : qManager, 'name':q];
                trace("mq.queue", it.get(CMQC.MQIA_CURRENT_Q_DEPTH), namespace, ['stat':'depth']);
                trace("mq.queue", it.get(CMQC.MQIA_OPEN_INPUT_COUNT), namespace, ['stat':'openinput']);
                trace("mq.queue", it.get(CMQC.MQIA_OPEN_OUTPUT_COUNT), namespace, ['stat':'openoutput']);
                trace("mq.queue", it.get(CMQCFC.MQIACF_UNCOMMITTED_MSGS), namespace, ['stat':'uncommited']);
                trace("mq.queue", it.get(CMQCFC.MQIACF_OLDEST_MSG_AGE), namespace, ['stat':'ageoom']);
                try {
                    long[] onQTimes = it.get(1226);                                              // Indicator of the time that messages remain on the queue
                    trace("mq.queue", onQTimes[0], namespace, ['stat':'onqtime-recent']);        // A value based on recent activity over a short period of time, microseconds
                    trace("mq.queue", onQTimes[1], namespace, ['stat':'onqtime']);               // A value based on activity over a longer period of time, microseconds
                } catch (e) {}
                
                try {
                    if("PTMS.TO.ECS.LQUEUE"==q) {
                        //println "Q: $q, Depth: ${it.get(CMQC.MQIA_CURRENT_Q_DEPTH)}, TIND: ${it.get(1226)}";            
                    }
                } catch (e) { println e; }
           } 
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

initChannelStatsMap = { channelStats ->    
    Map<String, Long> statsMap = new HashMap<String, Long>();
    statsMap.putAll(['buffersreceived':0, 'bufferssent':0,'bytesreceived':0, 'bytessent':0]);
    Set<String> cNames = new HashSet<String>();
    channelStats.each() {
        cNames.add(it.get(MQConstants.MQCACH_CHANNEL_NAME).trim());
    }
    Map<String, Map<String, Long>> statsAccumulator = new HashMap<String, Map<String, Long>>(cNames.size());
    cNames.each() {
        statsAccumulator.put(it, new HashMap<String, Long>(statsMap));
    }
    return statsAccumulator;
}

doChannelStats = {
    // =================================================
    // Get Channel Stats
    // =================================================
    
    time({
        List channelStats = request(false, pcf, MQConstants.MQCMD_INQUIRE_CHANNEL_STATUS, [(MQConstants.MQCACH_CHANNEL_NAME):"*"]);
        Map<String, Map<String, Long>> statsAccumulator = initChannelStatsMap(channelStats);        
        Map<String, String> channelTypes = new HashMap<String, String>(statsAccumulator.size());
        channelStats.each() {
            channelName = it.get(MQConstants.MQCACH_CHANNEL_NAME).trim();            
            channelType = resolveChType(it.get(MQConstants.MQIACH_CHANNEL_TYPE));
            if(!channelTypes.containsKey(channelName)) {
                channelTypes.put(channelName, channelType);
            }
            /*
            connectionName = it.get(MQConstants.MQCACH_CONNECTION_NAME);
            xmitQueueName = it.get(MQConstants.MQCACH_XMIT_Q_NAME);
            disposition = it.get(MQConstants.MQIACH_CHANNEL_DISP);
            instanceType = it.get(MQConstants.MQIACH_CHANNEL_INSTANCE_TYPE);
            */
            Map<String, Long> statsMap = statsAccumulator.get(channelName);            
            //println "Channel:\n\tName: $channelName\n\tConnection Name: $connectionName\n\tXMIT Queue: $xmitQueueName\n\tChannel Type: $channelType\n\tDisposition: $disposition\n\tInstance Type: $instanceType";            
            statsMap.put('buffersreceived', statsMap.get('buffersreceived')+it.get(MQConstants.MQIACH_BUFFERS_RCVD));
            statsMap.put('bufferssent', statsMap.get('bufferssent')+it.get(MQConstants.MQIACH_BUFFERS_SENT));
            statsMap.put('bytesreceived', statsMap.get('bytesreceived')+Math.round((it.get(MQConstants.MQIACH_BYTES_RCVD)/1024)));
            statsMap.put('bytessend', statsMap.get('bytessent')+Math.round((it.get(MQConstants.MQIACH_BYTES_SENT)/1024)));
            statsMap.put('bytessent', statsMap.get('bytessent')+it.get(MQConstants.MQIACH_BYTES_SENT));

        }
        
        channelTypes.each() { channelName, channelType ->
            def namespace = ['qmanager' : qManager, 'name': channelName, 'type':channelType];
            statsMap = statsAccumulator.get(channelName);
            trace("mq.channel", statsMap.get('buffersreceived'), namespace, ['unit':'buffers', 'dir':'received']); //"Buffers Received", "WebSphere MQ", qManager, "Channels", channelType, channelName);
            trace("mq.channel", statsMap.get('bufferssent'), namespace, ['unit':'buffers', 'dir':'sent']); // , "Buffers Sent", "WebSphere MQ", qManager, "Channels", channelType, channelName);
            trace("mq.channel", statsMap.get('bytesreceived'), namespace, ['unit':'kbytes', 'dir':'received']);  // , "Bytes Received", "WebSphere MQ", qManager, "Channels", channelType, channelName);
            trace("mq.channel", statsMap.get('bytessent'), namespace, ['unit':'kbytes', 'dir':'sent']);  // "Bytes Sent", "WebSphere MQ", qManager, "Channels", channelType, channelName);                    
        }
      }, "mq.monitor", ["qmanager":qManager, 'stat':'elapsed', 'monitor':'channelstats']);
}

doTopicStats = {
    // =================================================
    // Get Topic Stats
    // =================================================
     time({
         request(false, pcf, CMQCFC.MQCMD_INQUIRE_TOPIC_STATUS, [(CMQC.MQCA_TOPIC_STRING):"#", (CMQCFC.MQIACF_TOPIC_STATUS_TYPE):CMQCFC.MQIACF_TOPIC_SUB]).each() {     
             byte[] subId = it.get(CMQCFC.MQBACF_SUB_ID);
             byte[] alt = new byte[16];
             
             System.arraycopy(subId, 0, alt, 0, 16);
             request(true, pcf, CMQCFC.MQCMD_INQUIRE_SUBSCRIPTION, [(CMQCFC.MQBACF_SUB_ID):subId]).each() {  
                String subName = it.get("MQCACF_SUB_NAME").trim();
                println getHexString(subName.getBytes());
                it.each() { k, v ->
                    //println
                    //println connectionId;
                    println "\t${k}:${v}";
                }                         
             }
         }
    }, "mq.monitor", ["qmanager":qManager, 'stat':'elapsed', 'monitor':'topicstats']); 
}    

try {
    tsdbSocket = new Socket("tsdb", 8080);
    println "TSDB Connected";
    pcf = new PCFMessageAgent("localhost", 1415, "JBOSS.SVRCONN")
    println "PCF Connected";
    qManager = pcf.getQManagerName();
    println "QManager: $qManager";
    //flush();
    while(true) {
        long start = System.currentTimeMillis();
        //  ===============================================================================
        //  Start Monitoring
        //  ===============================================================================

        try {
//            doGetQueueNames();
//            doGetQueueMaxDepths();
//            doQueueStats();
//            doChannelStats();
                doTopicStats();
        } catch (e) {
            e.printStackTrace(System.err);
        } finally {
            pflush();
            break;
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