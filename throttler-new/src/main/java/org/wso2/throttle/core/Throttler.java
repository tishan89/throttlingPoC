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
import org.wso2.siddhi.core.util.EventPrinter;
import org.wso2.throttle.common.util.DatabridgeServerUtil;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility class which does throttling
 */
public class Throttler {
    private static final Logger log = Logger.getLogger(Throttler.class);
    static Throttler throttler;

    private SiddhiManager siddhiManager;
    private InputHandler ruleStreamInputHandler;
    private InputHandler requestStreamInputHandler;
    private InputHandler globalStreamInputHandler;
    private EventReceivingServer eventReceivingServer;
    private static Map<String, ResultContainer> resultMap = new ConcurrentHashMap<String, ResultContainer>();
    private int ruleCount = 0;

    private static String hostName = "10.100.5.99";
    private DataPublisher dataPublisher = null;

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

        String commonExecutionPlan = "define stream RuleStream (rule string, v1 string, v2 string, messageID string);\n" +
                                     "define stream GlobalResultStream (key string, isThrottled bool);\n" +
                                     "\n" +
                                     "@IndexBy('key') \n" +
                                     "define table ThrottleTable (key string, isThrottled bool);\n" +
                                     "\n" +
                                     "/* COMMON QUERIES BEGIN \n" +
                                     "These queries will not change as new rules are added/removed.\n" +
                                     "*/\n" +
                                     "from RuleStream join ThrottleTable\n" +
                                     "on ThrottleTable.key == str:concat(RuleStream.rule, \"_\", RuleStream.v1, \"_\", RuleStream.v2)\n" +
                                     "select RuleStream.rule, RuleStream.v1, RuleStream.v2, ThrottleTable.isThrottled, RuleStream.messageID\n" +
                                     "insert into LocalResultStream;\n" +
                                     "\n" +
                                     "from RuleStream[not ((str:concat(RuleStream.rule, \"_\", RuleStream.v1, \"_\", RuleStream.v2) == ThrottleTable.key ) in ThrottleTable)]\n" +
                                     "select RuleStream.rule as rule, RuleStream.v1, RuleStream.v2, false as isThrottled, RuleStream.messageID\n" +
                                     "insert into LocalResultStream;\n" +
                                     "/* COMMON QUERIES END */\n" +
                                     "\n" +
                                     "/* Updating Throttle Table with the outputs coming from the global CEP */\n" +
                                     "from GlobalResultStream\n" +
                                     "select *\n" +
                                     "insert into ThrottleTable;";

        ExecutionPlanRuntime commonExecutionPlanRuntime = siddhiManager.createExecutionPlanRuntime(commonExecutionPlan);

        //add any callbacks
        commonExecutionPlanRuntime.addCallback("LocalResultStream", new StreamCallback() {
            @Override
            public void receive(Event[] events) {
                EventPrinter.print(events);
                //Get corresponding result container and add the result
                for (Event event : events) {
                    resultMap.get(event.getData(4).toString()).addResult((Boolean) event.getData(3));
                }
            }
        });

        //get and register inputHandler
        setRuleStreamInputHandler(commonExecutionPlanRuntime.getInputHandler("RuleStream"));
        setGlobalStreamInputHandler(commonExecutionPlanRuntime.getInputHandler("GlobalResultStream"));

        //start common EP Runtime
        commonExecutionPlanRuntime.start();

        eventReceivingServer = new EventReceivingServer();
        eventReceivingServer.start(9611, 9711);

        initDataPublisher();
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

        //Add call backs. Here, we take output events and insert into RuleStream
        ruleRuntime.addCallback("RuleStream", new StreamCallback() {
            @Override
            public void receive(Event[] events) {
                try {
                    getRuleStreamInputHandler().send(events);
                } catch (InterruptedException e) {
                    log.error("Error occurred when publishing to RuleStream.", e);
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

    private InputHandler getRuleStreamInputHandler() {
        return ruleStreamInputHandler;
    }

    private void setRuleStreamInputHandler(InputHandler ruleStreamInputHandler) {
        this.ruleStreamInputHandler = ruleStreamInputHandler;
    }

    private InputHandler getRequestStreamInputHandler() {
        return requestStreamInputHandler;
    }

    private void setRequestStreamInputHandler(InputHandler requestStreamInputHandler) {
        this.requestStreamInputHandler = requestStreamInputHandler;
    }

    public InputHandler getGlobalStreamInputHandler() {
        return globalStreamInputHandler;
    }

    private void setGlobalStreamInputHandler(InputHandler globalStreamInputHandler) {
        this.globalStreamInputHandler = globalStreamInputHandler;
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
