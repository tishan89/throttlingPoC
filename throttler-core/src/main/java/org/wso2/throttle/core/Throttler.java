/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
import org.wso2.carbon.databridge.agent.DataPublisher;
import org.wso2.carbon.databridge.agent.exception.DataEndpointAgentConfigurationException;
import org.wso2.carbon.databridge.agent.exception.DataEndpointAuthenticationException;
import org.wso2.carbon.databridge.agent.exception.DataEndpointConfigurationException;
import org.wso2.carbon.databridge.agent.exception.DataEndpointException;
import org.wso2.carbon.databridge.commons.exception.TransportException;
import org.wso2.carbon.databridge.commons.utils.DataBridgeCommonsUtils;
import org.wso2.siddhi.core.ExecutionPlanRuntime;
import org.wso2.siddhi.core.SiddhiManager;
import org.wso2.siddhi.core.event.Event;
import org.wso2.siddhi.core.stream.input.InputHandler;
import org.wso2.siddhi.core.stream.output.StreamCallback;
import org.wso2.throttle.api.Policy;
import org.wso2.throttle.exception.ThrottleConfigurationException;
import org.wso2.throttle.util.ThrottleHelper;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Class which does throttling.
 * 1. Get an instance
 * 2. Start
 * 3. Add rules
 * 4. Invoke isThrottled with {@link org.wso2.throttle.core.Throttler} object
 */
public class Throttler {
    private static final Logger log = Logger.getLogger(Throttler.class);

    private static Throttler throttler;

    private SiddhiManager siddhiManager;
    private InputHandler eligibilityStreamInputHandler;
    private List<InputHandler> requestStreamInputHandlerList = new CopyOnWriteArrayList<InputHandler>();
    private static Map<String, ResultContainer> resultMap = new ConcurrentHashMap<String, ResultContainer>();

    private ExecutionPlanRuntime ruleRuntime;
    private int ruleCount = 0;


    private DataPublisher dataPublisher = null;
    private String streamID;

    private CEPConfig cepConfig;
    private Throttler() {
    }

    public static synchronized Throttler getInstance() {
        if (throttler == null) {
            throttler = new Throttler();
            throttler.start();
        }
        return throttler;
    }

    /**
     * Starts throttler engine. Calling method should catch the exceptions and call stop to clean up.
     */
    private void start() {
        siddhiManager = new SiddhiManager();
        ThrottleHelper.loadDataSourceConfiguration(siddhiManager);

        String commonExecutionPlan = "" +
                "define stream EligibilityStream (rule string, messageID string, isEligible bool, key string);\n" +
                "\n" +
                "@From(eventtable='rdbms', datasource.name='org_wso2_throttle_DataSource', " +
                "table.name='ThrottleTable', bloom.filters = 'enable', bloom.validity='100')" +
                "define table ThrottleTable (THROTTLE_KEY string, isThrottled bool);\n" +
                "\n" +
                "FROM EligibilityStream[isEligible==false]\n" +
                "SELECT rule, messageID, false AS isThrottled\n" +
                "INSERT INTO ThrottleStream;\n" +
                "\n" +
                "FROM EligibilityStream[isEligible==true]#window.length(1) LEFT OUTER JOIN ThrottleTable\n" +
                "\tON ThrottleTable.THROTTLE_KEY == EligibilityStream.key\n" +
                "SELECT rule, messageID, ifThenElse((ThrottleTable.isThrottled is null),false,ThrottleTable.isThrottled) AS isThrottled\n" +
                "INSERT INTO ThrottleStream;";

        ExecutionPlanRuntime commonExecutionPlanRuntime = siddhiManager.createExecutionPlanRuntime(commonExecutionPlan);

        //add callback to get local throttling result and add it to ResultContainer
        commonExecutionPlanRuntime.addCallback("ThrottleStream", new StreamCallback() {
            @Override
            public void receive(Event[] events) {
                for (Event event : events) {
                    resultMap.get(event.getData(1).toString()).addResult((String) event.getData(0), (Boolean) event.getData(2));
                }
            }
        });

        //get and register inputHandler
        this.eligibilityStreamInputHandler = commonExecutionPlanRuntime.getInputHandler("EligibilityStream");

        commonExecutionPlanRuntime.start();

        try {
            populateThrottlingPolicies();
            cepConfig = ThrottleHelper.loadCEPConfig();
        } catch (ThrottleConfigurationException e) {
            log.error("Error in initializing throttling engine. " + e.getMessage(), e);
        }
        
        deployLocalCEPRules();      //todo ideally caller should call this in future

        //initialize binary data publisher to send requests to global CEP instance
        initDataPublisher();
    }



    /**
     * Reads throttling policy file and load {@link org.wso2.throttle.core.QueryTemplateStore}
     * @throws ThrottleConfigurationException
     */
    private void populateThrottlingPolicies() throws ThrottleConfigurationException {
        for(Policy policy : ThrottleHelper.loadThrottlingPolicies()){
            QueryTemplateStore.getInstance().addThrottlingEligibilityQuery(policy.getEligibilityQuery());
        }
    }

//    //todo: this method has not being implemented completely. Will be done after doing perf tests.
//    private void deployRuleToGlobalCEP(String templateID, String parameter1, String parameter2) {
//        String queries = QueryTemplateStore.constructEnforcementQuery();
//
//        ExecutionPlanRuntime ruleRuntime = siddhiManager.createExecutionPlanRuntime("define stream RequestStream (messageID string, app_key string, api_key string, resource_key string, app_tier string, api_tier string, resource_tier string); " +
//                queries);
//
//        GlobalCEPClient globalCEPClient = new GlobalCEPClient();
//        globalCEPClient.deployExecutionPlan(queries);
//    }

    public void deployLocalCEPRules() {
        StringBuilder eligibilityQueriesBuilder = new StringBuilder();
        eligibilityQueriesBuilder.append("define stream RequestStream (" + QueryTemplateStore.getInstance()
                .loadThrottlingAttributes() + "); \n");

        for (String eligibilityQuery : QueryTemplateStore.getInstance().loadThrottlingEligibilityQueries()) {
            eligibilityQueriesBuilder.append(eligibilityQuery);
            ruleCount++;
        }


        ExecutionPlanRuntime ruleRuntime = siddhiManager.createExecutionPlanRuntime(
                eligibilityQueriesBuilder.toString());

        //Add call backs. Here, we take output events and insert into EligibilityStream
        ruleRuntime.addCallback("EligibilityStream", new StreamCallback() {
            @Override
            public void receive(Event[] events) {
                try {
                    getEligibilityStreamInputHandler().send(events);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("Error occurred when publishing to EligibilityStream.", e);
                }
            }
        });

        //get and register input handler for RequestStream, so isThrottled() can use it.
        requestStreamInputHandlerList.add(ruleRuntime.getInputHandler("RequestStream"));
        ruleRuntime.start();
    }


    /**
     * Returns whether the given throttleRequest is throttled.
     *
     * @param throttleRequest User throttleRequest to APIM which needs to be checked whether throttled
     * @return Throttle status for current throttleRequest
     */
    public boolean isThrottled(Object[] throttleRequest) {
        if (ruleCount != 0) {
            String uniqueKey = (String) throttleRequest[0];
            ResultContainer result = new ResultContainer(ruleCount);
            resultMap.put(uniqueKey.toString(), result);
            Iterator<InputHandler> handlerList = requestStreamInputHandlerList.iterator();
            while (handlerList.hasNext()) {
                InputHandler inputHandler = handlerList.next();
                try {
                    inputHandler.send(Arrays.copyOf(throttleRequest, throttleRequest.length));
                } catch (InterruptedException e) {
                    //interrupt current thread so that interrupt can propagate
                    Thread.currentThread().interrupt();
                    log.error(e.getMessage(), e);
                }
            }
            //Blocked call to return synchronous result
            boolean isThrottled = false;
            try {
                isThrottled = result.isThrottled();
                log.info("Throttling status for request to API " + throttleRequest[2] + " is " + isThrottled);
            } catch (InterruptedException e) {
                //interrupt current thread so that interrupt can propagate
                Thread.currentThread().interrupt();
                log.error(e.getMessage(), e);
            }
            if (!isThrottled) {                                           //Only send served throttleRequest to global throttler
                sendToGlobalThrottler(throttleRequest);
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
        if (ruleRuntime != null) {
            ruleRuntime.shutdown();
        }
    }


    private InputHandler getEligibilityStreamInputHandler() {
        return eligibilityStreamInputHandler;
    }

    private void sendToGlobalThrottler(Object[] throttleRequest) {
        org.wso2.carbon.databridge.commons.Event event = new org.wso2.carbon.databridge.commons.Event();
        event.setStreamId(streamID);
        event.setMetaData(null);
        event.setCorrelationData(null);
        event.setPayloadData(throttleRequest);
        dataPublisher.tryPublish(event);
    }

    //todo exception handling
    private void initDataPublisher() {
        try {
            dataPublisher = new DataPublisher("Binary", "tcp://" + cepConfig.getHostname() + ":" + cepConfig.getBinaryTCPPort(),
                    "ssl://" + cepConfig.getHostname() + ":" + cepConfig.getBinarySSLPort(), cepConfig.getUsername(),
                    cepConfig.getPassword());
            streamID = DataBridgeCommonsUtils.generateStreamId(cepConfig.getStreamName(), cepConfig.getStreamVersion());
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
