/*
 * Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.throttle.core;

import org.apache.log4j.Logger;
import org.wso2.carbon.databridge.agent.AgentHolder;
import org.wso2.carbon.databridge.agent.DataPublisher;
import org.wso2.carbon.databridge.agent.exception.DataEndpointAgentConfigurationException;
import org.wso2.carbon.databridge.agent.exception.DataEndpointAuthenticationException;
import org.wso2.carbon.databridge.agent.exception.DataEndpointConfigurationException;
import org.wso2.carbon.databridge.agent.exception.DataEndpointException;
import org.wso2.carbon.databridge.commons.exception.TransportException;
import org.wso2.carbon.databridge.commons.utils.DataBridgeCommonsUtils;
import org.wso2.carbon.databridge.core.exception.DataBridgeException;
import org.wso2.carbon.databridge.core.exception.StreamDefinitionStoreException;
import org.wso2.siddhi.core.ExecutionPlanRuntime;
import org.wso2.siddhi.core.SiddhiManager;
import org.wso2.siddhi.core.event.Event;
import org.wso2.siddhi.core.stream.input.InputHandler;
import org.wso2.siddhi.core.stream.output.StreamCallback;
import org.wso2.throttle.common.util.DatabridgeServerUtil;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Utility class which does throttling
 */
public class Throttler {
    private static final Logger log = Logger.getLogger(Throttler.class);
    static Throttler throttler;

    private SiddhiManager siddhiManager;
    private InputHandler eligibilityStreamInputHandler;
    private InputHandler requestStreamInputHandler;
    private InputHandler globalThrottleStreamInputHandler;
    private EventReceivingServer eventReceivingServer;
    private static Map<String, ResultContainer> resultMap = new ConcurrentHashMap<String, ResultContainer>();
    private int ruleCount = 0;

    private String hostName = "localhost";      //10.100.5.99
    private DataPublisher dataPublisher = null;

    //perf test variables
    private AtomicLong counter = new AtomicLong(0);
    private AtomicLong lastCounter = new AtomicLong(0);
    private AtomicLong lastIndex = new AtomicLong(0);
    private AtomicBoolean calcInProgress = new AtomicBoolean(false);
    private AtomicLong lastTime = new AtomicLong(System.currentTimeMillis());
    private DecimalFormat decimalFormat = new DecimalFormat("#.##");
    private int elapsedCount = 10;

    private Throttler() {
    }

    public static synchronized Throttler getInstance() {
        if (throttler == null) {
            throttler = new Throttler();
        }
        return throttler;
    }

    /**
     * Starts throttler engine. Calling method should catch the exceptions and call stop to clean up.
     */
    public void start() throws DataBridgeException, IOException, StreamDefinitionStoreException {
        siddhiManager = new SiddhiManager();

        String commonExecutionPlan = "define stream EligibilityStream (rule string, messageID string, isEligible bool, key string, v1 string, v2 string);\n" +
                                     "define stream GlobalThrottleStream (key string, isThrottled bool); \n" +
                                     "\n" +
                                     "@IndexBy('key')\n" +
                                     "define table ThrottleTable (key string, isThrottled bool);\n" +
                                     "\n" +
                                     "FROM EligibilityStream[isEligible==false]\n" +
                                     "SELECT rule, messageID, false AS isThrottled\n" +
                                     "INSERT INTO ThrottleStream;\n" +
                                     "\n" +
                                     "FROM EligibilityStream[isEligible==true]\n" +
                                     "SELECT rule, messageID, isEligible, key, v1, v2\n" +
                                     "INSERT INTO EligibileStream;\n" +
                                     "\n" +
                                     "FROM EligibileStream JOIN ThrottleTable\n" +
                                     "\tON ThrottleTable.key == EligibileStream.key\n" +
                                     "SELECT rule, messageID, ThrottleTable.isThrottled AS isThrottled\n" +
                                     "INSERT INTO ThrottleStream;\n" +
                                     "\n" +
                                     "from GlobalThrottleStream\n" +
                                     "select *\n" +
                                     "insert into ThrottleTable;";

        ExecutionPlanRuntime commonExecutionPlanRuntime = siddhiManager.createExecutionPlanRuntime(commonExecutionPlan);

        //add any callbacks
        commonExecutionPlanRuntime.addCallback("ThrottleStream", new StreamCallback() {
            @Override
            public void receive(Event[] events) {
//                EventPrinter.print(events);
                //Get corresponding result container and add the result
                for (Event event : events) {
                    resultMap.get(event.getData(1).toString()).addResult((Boolean) event.getData(2));
                }
                calcThroughput(events);
            }
        });

        //get and register inputHandler
        setEligibilityStreamInputHandler(commonExecutionPlanRuntime.getInputHandler("EligibilityStream"));
        setGlobalThrottleStreamInputHandler(commonExecutionPlanRuntime.getInputHandler("GlobalThrottleStream"));

        //start common EP Runtime
        commonExecutionPlanRuntime.start();

        eventReceivingServer = new EventReceivingServer();
        eventReceivingServer.start(9611, 9711);

        initDataPublisher();
    }

    private void calcThroughput(Event[] events) {
        long currentTime = System.currentTimeMillis();
        long localCounter = counter.addAndGet(events.length);
        long index = localCounter / elapsedCount;
        if (lastIndex.get() != index) {
            if (calcInProgress.compareAndSet(false, true)) {
                //TODO Can be made thread safe further
                lastIndex.set(index);
                long currentWindowEventsReceived = localCounter - lastCounter.getAndSet(localCounter);
                //log.info("Current time: " + System.currentTimeMillis() + ", Event received time: " + currentTime + ", Last calculation time: " + lastTime.get());
                long elapsedTime = currentTime - lastTime.getAndSet(currentTime);
                double throughputPerSecond = (((double) currentWindowEventsReceived) / elapsedTime) * 1000;

                log.info("[" + Thread.currentThread().getName() + "] Received " + currentWindowEventsReceived + " sensor events in " + elapsedTime
                         + " milliseconds with total throughput of " + decimalFormat.format(throughputPerSecond)
                         + " events per second.");
                calcInProgress.set(false);
            }
        }
    }

    /**
     * This method lets a user to add a predefined rule (pre-defined as a template), specifying desired parameters.
     *
     * @param templateID ID of the rule-template.
     * @param parameter1 First parameter, to be inserted in to the template
     * @param parameter2 Second parameter, to be inserted in to the template
     */
    public synchronized void addRule(String templateID, String parameter1, String parameter2) {
        deployRuleToLocalCEP(templateID, parameter1, parameter2);
//        deployRuleToGlobalCEP(templateID, parameter1, parameter2);  //todo: test after doing perf tests.
    }

    //todo: this method has not being implemented completely. Will be done after doing perf tests.
    private void deployRuleToGlobalCEP(String templateID, String parameter1, String parameter2){
        //get rule-query from templateIDToQuery map
        String queryTemplate = GlobalTemplateStore.getInstance().getQueryTemplate(templateID);
        if (queryTemplate == null) {
            throw new RuntimeException("No query template exist for ID: " + templateID + " in Global Template Store.");
        }

        //replace parameters in the queries, if required.
        String queries = replaceParamsInTemplate(queryTemplate, parameter1, parameter2);

        //create execution plan runtime with the query created above
        ExecutionPlanRuntime ruleRuntime = siddhiManager.createExecutionPlanRuntime("define stream RequestStream (apiName string, userID string, messageID string); " +
                                                                                    queries);

        //get global CEP client
        GlobalCEPClient globalCEPClient = new GlobalCEPClient();
        globalCEPClient.deployExecutionPlan(queries);
    }

    private void deployRuleToLocalCEP(String templateID, String parameter1, String parameter2){
        //get rule-query from templateIDToQuery map
        String queryTemplate = TemplateStore.getInstance().getQueryTemplate(templateID);
        if (queryTemplate == null) {
            throw new RuntimeException("No query template exist for ID: " + templateID + " in Local Template Store.");
        }

        //replace parameters in the query, if required.
        String query = replaceParamsInTemplate(queryTemplate, parameter1, parameter2);

        //create execution plan runtime with the query created above
        ExecutionPlanRuntime ruleRuntime = siddhiManager.createExecutionPlanRuntime("define stream RequestStream (apiName string, userID string, messageID string); " +
                                                                                    query);

        //Add call backs. Here, we take output events and insert into EligibilityStream
        ruleRuntime.addCallback("EligibilityStream", new StreamCallback() {
            @Override
            public void receive(Event[] events) {
                try {
                    getEligibilityStreamInputHandler().send(events);
                } catch (InterruptedException e) {
                    log.error("Error occurred when publishing to EligibilityStream.", e);
                }
            }
        });

        //get and register input handler for RequestStream, so isThrottled() can use it.
        setRequestStreamInputHandler(ruleRuntime.getInputHandler("RequestStream"));
        //Need to know current rule count to provide synchronous API
        ruleCount++;
        ruleRuntime.start();
    }

    //todo
    public synchronized void removeRule() {
    }


    /**
     * Returns whether the given request is throttled.
     *
     * @param request User request to APIM which needs to be checked whether throttled
     * @return Throttle status for current status
     * @throws InterruptedException
     */
    public boolean isThrottled(Request request) throws InterruptedException {
        UUID uniqueKey = UUID.randomUUID();
        if (ruleCount == 0) {
            ResultContainer result = new ResultContainer(ruleCount);
            resultMap.put(uniqueKey.toString(), result);
            getRequestStreamInputHandler().send(new Object[]{request.getParameter1(), request.getParameter2(),
                    uniqueKey});
            //Blocked call to return synchronous result
            boolean isThrottled = result.isThrottled();
            if (!isThrottled) { //Only send served request to global throttler
                sendToGlobalThrottler(new Object[]{request.getParameter1(), request.getParameter2(), uniqueKey});
            }
            resultMap.remove(uniqueKey);
            return isThrottled;
        } else {
            return false;
        }
    }

    public void stop() {
        if (siddhiManager != null) {
            siddhiManager.shutdown();
        }
        if (eventReceivingServer != null) {
            eventReceivingServer.stop();
        }
    }


    //todo: improve validation
    private String replaceParamsInTemplate(String template, String parameter1, String parameter2) {
        if (template == null) {
            throw new IllegalArgumentException("template cannot be null");
        }
        if (parameter1 != null) {
            template = template.replace("$param1", "\"" + parameter1 + "\"");
        }
        if (parameter2 != null) {
            template = template.replace("$param2", "\"" + parameter2 + "\"");
        }
        return template;
    }

    private InputHandler getEligibilityStreamInputHandler() {
        return eligibilityStreamInputHandler;
    }

    private void setEligibilityStreamInputHandler(InputHandler eligibilityStreamInputHandler) {
        this.eligibilityStreamInputHandler = eligibilityStreamInputHandler;
    }

    private InputHandler getRequestStreamInputHandler() {
        return requestStreamInputHandler;
    }

    private void setRequestStreamInputHandler(InputHandler requestStreamInputHandler) {
        this.requestStreamInputHandler = requestStreamInputHandler;
    }

    public InputHandler getGlobalThrottleStreamInputHandler() {
        return globalThrottleStreamInputHandler;
    }

    private void setGlobalThrottleStreamInputHandler(InputHandler globalThrottleStreamInputHandler) {
        this.globalThrottleStreamInputHandler = globalThrottleStreamInputHandler;
    }

    private void sendToGlobalThrottler(Object[] data) {
        org.wso2.carbon.databridge.commons.Event event = new org.wso2.carbon.databridge.commons.Event();
        event.setStreamId(DataBridgeCommonsUtils.generateStreamId("org.wso2.throttle.request.stream", "1.0.0"));
        event.setMetaData(null);
        event.setCorrelationData(null);
        event.setPayloadData(new Object[]{data[0], data[1], data[2]});

        dataPublisher.publish(event);
    }

    private void initDataPublisher() {
        AgentHolder.setConfigPath(DatabridgeServerUtil.getDataAgentConfigPath());
        DatabridgeServerUtil.setTrustStoreParams();

        try {
            dataPublisher = new DataPublisher("Binary", "tcp://" + hostName + ":9621",
                                              "ssl://" + hostName + ":9721", "admin", "admin");
        } catch (DataEndpointAgentConfigurationException e) {
            log.error(e.getMessage(), e);
        } catch (DataEndpointException e) {
            log.error(e.getMessage(), e);
        } catch (DataEndpointConfigurationException e) {
            log.error(e.getMessage(), e);
        } catch (DataEndpointAuthenticationException e) {
            log.error(e.getMessage(), e);
        } catch (TransportException e) {
            log.error(e.getMessage(), e);
        }
    }

}
