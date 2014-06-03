package com.theice.perfsandbox.mqutils

import com.ibm.mq.MQQueueManager
import com.ibm.mq.constants.MQConstants
import java.text.SimpleDateFormat
import com.ibm.mq.pcf.*

/**
 * Created by IntelliJ IDEA.
 * User: jjordan1
 * Date: Oct 6, 2010
 * Time: 12:36:46 PM
 */
class PCFUtils {

    public PCFMessageAgent connectToMq(String hostname, int port, String channel) {
        return this.connectToMq(hostname, port, channel, "")

    }

    /**
     *
     * @param hostname
     * @param port
     * @param channel
     * @param queueManager
     * @return
     */
    private PCFMessageAgent connectToMq(String hostname, int port, String channel, String queueManager) {
        /*MQEnvironment.hostname = hostname
      MQEnvironment.port = port
      MQEnvironment.channel = channel*/

        def PCFMessageAgent agent = null
        def MQQueueManager qmgr = null

        try {
            //qmgr = new MQQueueManager(queueManager)
            agent = new PCFMessageAgent(hostname, port, channel)

        } catch (Exception e) {
            e.printStackTrace()

        }
        return agent
    }

    /**
     *
     * @param agent
     * @return
     */
    public String[] getQueues(PCFMessageAgent agent) {

        def PCFMessage request
        def PCFMessage[] responses
        def String[] names

        //CMQCFC.MQCMD

        request = new PCFMessage(CMQCFC.MQCMD_INQUIRE_Q_NAMES)
        request.addParameter(CMQC.MQCA_Q_NAME, "*")
        request.addParameter(CMQC.MQIA_Q_TYPE, CMQC.MQQT_LOCAL)

        responses = agent.send(request)
        names = (String[]) responses[0].getParameterValue(CMQCFC.MQCACF_Q_NAMES)
        return names
    }

    /**
     *
     * @param agent
     * @return
     */
    private String[] getTopics(PCFMessageAgent agent) {

        def PCFMessage request
        def PCFMessage[] responses
        def String[] names

        request = new PCFMessage(CMQCFC.MQCMD_INQUIRE_TOPIC_NAMES)
        request.addParameter(CMQC.MQCA_TOPIC_NAME, "*")
        //request.addParameter(CMQC.MQIA_TOPIC_TYPE, CMQC.MQTOPT_LOCAL)
        responses = agent.send(request)
        names = (String[]) responses[0].getParameterValue(CMQCFC.MQCACF_TOPIC_NAMES)
        return names
    }

    public String getMQStats(String hostname, int port, String channel, boolean prettyprint) {
        def PCFMessage request1 = null
        def PCFMessage request2 = null
        def PCFMessage[] responses1 = null
        def PCFMessage[] responses2 = null
        def String result = "";

        def PCFMessageAgent agent = connectToMq(hostname, port, channel)

        def String[] queues = getQueues(agent)

        def int[] mqiacf_attrs = [CMQC.MQCA_Q_NAME, CMQC.MQIA_CURRENT_Q_DEPTH, CMQC.MQIA_MAX_Q_DEPTH, CMQC.MQIA_OPEN_INPUT_COUNT, CMQC.MQIA_OPEN_OUTPUT_COUNT]

        def pattern = ~/SYSTEM.*/

        def int[] padding = [40, 11, 12, 13, 14]
        def header = ["Queue Name", "Depth", "Publishers", "Subscribers", "Oldest MsgAge"]
        def border = StringUtils.buildLine(["", "", "", "", "",], (char) '+', padding, (char) '-', false)
        if (prettyprint) {
            println border
            println StringUtils.buildLine(header, (char) '|', padding, (char) ' ', true)
            println border
        }
        queues.each() { queueName ->
            // Build the request
            //if (queueName == "PTMS.TO.ECS.DEFAULT.LQ") {
            if (!pattern.matcher(queueName).matches()) {
                //if(queueName.startsWith("ACT.TO.LOADER.LQUEUE")){
                try {

                    request1 = new PCFMessage(CMQCFC.MQCMD_INQUIRE_Q_STATUS)
                    request1.addParameter(CMQC.MQCA_Q_NAME, queueName)
                    request1.addParameter(CMQC.MQIA_Q_TYPE, CMQC.MQQT_LOCAL)
                    //request1.addParameter(CMQCFC.MQIACF_Q_ATTRS, mqiacf_attrs)


                    request2 = new PCFMessage(CMQCFC.MQCMD_RESET_Q_STATS)
                    request2.addParameter(CMQC.MQCA_Q_NAME, queueName)

                    responses1 = agent.send(request1)
                    responses2 = agent.send(request2)

                    /*responses1.each {response ->
                      println response
                    }*/
                    def columns = []
                    long time = System.currentTimeMillis();
                    responses1.each {response ->
                        columns.add(response.getStringParameterValue(CMQC.MQCA_Q_NAME).trim())
                        columns.add(String.valueOf(response.getIntParameterValue(CMQC.MQIA_CURRENT_Q_DEPTH)))
                        columns.add(String.valueOf(response.getIntParameterValue(CMQC.MQIA_OPEN_OUTPUT_COUNT)))
                        columns.add(String.valueOf(response.getIntParameterValue(CMQC.MQIA_OPEN_INPUT_COUNT)))
                        columns.add(String.valueOf(response.getIntParameterValue(CMQCFC.MQIACF_OLDEST_MSG_AGE)))
                        //columns.add(String.valueOf(response.getIntListParameterValue(CMQCFC.MQIACF_Q_TIME_INDICATOR)[0]))
                        //columns.add(String.valueOf(response.getIntListParameterValue(CMQCFC.MQIACF_Q_TIME_INDICATOR)[1]))
                        if (prettyprint) {
                            println StringUtils.buildLine(columns, (char) '|', padding, (char) ' ', false)
                        } else {

                            def output = "$time|"
                            columns.each {column ->
                                output += column;
                                output += "|"
                            }
                            println output
                        }
                    }

                } catch (Exception e) {
                    result += "$queueName failed - ${e.message}\r\n"
                }
                //}
            }
        }
        if (prettyprint) {
            println border
        }

        agent.disconnect()
        return result
    }

    /**
     *
     * @param hostname
     * @param port
     * @param channel
     * @param queueManager
     * @throws Exception
     */
    public void channelStatus(String hostname, int port, String channel, String queueManager) throws Exception {
        int chlCount = 0;

        PCFMessageAgent agent;
        PCFMessage request;
        PCFMessage[] responses;

        request = new PCFMessage(MQConstants.MQCMD_INQUIRE_CHANNEL_STATUS);
        request.addParameter(MQConstants.MQCACH_CHANNEL_NAME, "*");

        try {
            // connect to the queue manager using the PCFMessageAgent
            agent = connectToMq(hostname, port, channel, queueManager)
        }
        catch (Exception e) {
            throw e;
        }
        try {

            // send the request and collect the responses
            responses = agent.send(request);
            def header = ["Channel", "Type", "Application", "Client", "Date", "Time", "ChannelStatus"]
            def int[] padding = [20, 8, 40, 20, 12, 8, 12]


            println StringUtils.buildLine(["", "", "", "", "", "", ""], (char) '+', padding, (char) '-', false)
            println StringUtils.buildLine(header, (char) '|', padding, (char) ' ', true)
            println StringUtils.buildLine(["", "", "", "", "", "", ""], (char) '+', padding, (char) '-', false)


            /*responses.each {response->
              println response
            }*/
            for (int i = 0; i < responses.length; i++) {
                String[] channelTypes = ["", "SDR", "SVR", "RCVR", "RQSTR", "", "CLTCN", "SVRCN", "CLUSRCVR", "CLUSSDR", ""]
                def columns = []
                try{
                columns.add(responses[i].getStringParameterValue(MQConstants.MQCACH_CHANNEL_NAME).trim())
                String type = channelTypes[responses[i].getIntParameterValue(MQConstants.MQIACH_CHANNEL_TYPE)]
                def app = ""
                if (type.trim() == "SDR" || type.trim() == "RCVR") {
                    app = ""
                } else {
                    app = responses[i].getStringParameterValue(MQConstants.MQCACH_REMOTE_APPL_TAG).trim()
                }
                columns.add(type)
                columns.add(app)
                columns.add(responses[i].getStringParameterValue(MQConstants.MQCACH_CONNECTION_NAME).trim())
                columns.add(responses[i].getStringParameterValue(MQConstants.MQCACH_CHANNEL_START_DATE).trim())
                columns.add(responses[i].getStringParameterValue(MQConstants.MQCACH_CHANNEL_START_TIME).trim())
                columns.add(this.resolveChStatus(responses[i].getIntParameterValue(MQConstants.MQIACH_CHANNEL_STATUS)))

                println StringUtils.buildLine(columns, (char) '|', padding, (char) ' ', false)
                }catch(Exception e){
                    println e.getMessage();
                }
            }
            println StringUtils.buildLine(["", "", "", "", "", "", ""], (char) '+', padding, (char) '-', false)
        }
        catch (PCFException pcfEx) {
            pcfEx.printStackTrace()
            println pcfEx.getMessage()
        }

        agent.disconnect()
    }

    /**
     *
     * @param hostname
     * @param port
     * @param channel
     * @param queueManager
     * @throws Exception
     */
    public void connectionStatus(String hostname, int port, String channel, String queueManager) throws Exception {
        int chlCount = 0;

        PCFMessageAgent agent;
        PCFMessage request;
        PCFMessage[] responses;

        int[] array = [CMQCFC.MQIACF_ALL]
        request = new PCFMessage(MQConstants.MQCMD_INQUIRE_CONNECTION);
        request.addParameter(CMQCFC.MQBACF_GENERIC_CONNECTION_ID, new byte[0]);
        request.addParameter(CMQCFC.MQIACF_CONNECTION_ATTRS, array);

        try {
            // connect to the queue manager using the PCFMessageAgent
            agent = connectToMq(hostname, port, channel, queueManager)
        }
        catch (Exception e) {
            throw e;
        }
        try {

            // send the request and collect the responses
            responses = agent.send(request);
            responses.each {response ->
                println response
            }
        
        }
        catch (PCFException pcfEx) {
            pcfEx.printStackTrace()
            println pcfEx.getMessage()
        }
    }

    def String getTime(String dateformat) {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat(dateformat);
        return sdf.format(cal.getTime());
    }

    
    public String resolveChStatus(int status){
        Map statues = [:]
        statues.put(0, 'INACTIVE')
        statues.put(1, 'BINDING')
        statues.put(2, 'STARTING')
        statues.put(3, 'RUNNING')
        statues.put(4, 'STOPPING')
        statues.put(5, 'RETRYING')
        statues.put(6, 'STOPPED')
        statues.put(7, 'REQUESTING')
        statues.put(8, 'PAUSED')
        statues.put(13, 'INITIALIZING')

        return statues.get(status)

    }
}
