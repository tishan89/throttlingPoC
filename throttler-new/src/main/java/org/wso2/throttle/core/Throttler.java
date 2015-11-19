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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Class which does throttling.
 * 1. Get an instance
 * 2. Start
 * 3. Add rules
 * 4. Invoke isThrottled
 */
public class Throttler {
    private static final Logger log = Logger.getLogger(Throttler.class);
    static Throttler throttler;

    private SiddhiManager siddhiManager;
    private InputHandler requestStreamInputHandler;
    private InputHandler globalThrottleStreamInputHandler;
    private EventReceivingServer eventReceivingServer;
    private static Map<String, ResultContainer> resultMap = new ConcurrentHashMap<String, ResultContainer>();
    private int ruleCount = 1;

    private String hostName = "localhost";      //10.100.5.99
    private DataPublisher dataPublisher = null;

    private Throttler() throws StreamDefinitionStoreException, IOException, DataBridgeException {
        start();
    }

    public static synchronized Throttler getInstance()
            throws StreamDefinitionStoreException, IOException, DataBridgeException {
        if (throttler == null) {
            throttler = new Throttler();
        }
        return throttler;
    }

    /**
     * Starts throttler engine.
     */
    private void start() throws DataBridgeException, IOException, StreamDefinitionStoreException {
        siddhiManager = new SiddhiManager();

        String executionPlan = "define stream RequestStream (messageID string, tier string, key string);\n" +
                               "\n" +
                               "define stream GlobalThrottleStream (key string, isThrottled bool); \n" +
                               "\n" +
                               "@IndexBy('key')\n" +
                               "define table ThrottleTable (key string, isThrottled bool);\n" +
                               "\n" +
                               "FROM RequestStream JOIN ThrottleTable\n" +
                               "\tON ThrottleTable.key == RequestStream.key\n" +
                               "SELECT rule, messageID, ThrottleTable.isThrottled AS isThrottled\n" +
                               "INSERT INTO ThrottleStream;\n" +
                               "\n" +
                               "from RequestStream[not ((RequestStream.key == ThrottleTable.key ) in ThrottleTable)]\n" +
                               "select RequestStream.rule as rule, RequestStream.messageID, false AS isThrottled\n" +
                               "insert into ThrottleStream;\n" +
                               "\n" +
                               "from GlobalThrottleStream\n" +
                               "select *\n" +
                               "insert into ThrottleTable;";

        ExecutionPlanRuntime executionPlanRuntime = siddhiManager.createExecutionPlanRuntime(executionPlan);

        //add any callbacks
        executionPlanRuntime.addCallback("ThrottleStream", new StreamCallback() {
            @Override
            public void receive(Event[] events) {
//                EventPrinter.print(events);
                //Get corresponding result container and add the result
                for (Event event : events) {
                    resultMap.get(event.getData(1).toString()).addResult((Boolean) event.getData(2));
                }
            }
        });

        //get and register inputHandler
        setRequestStreamInputHandler(executionPlanRuntime.getInputHandler("RequestStream"));
        setGlobalThrottleStreamInputHandler(executionPlanRuntime.getInputHandler("GlobalThrottleStream"));

        //start common EP Runtime
        executionPlanRuntime.start();

        InputHandler inputHandler = executionPlanRuntime.getInputHandler("GlobalThrottleStream");
        try {
            inputHandler.send(new Object[]{"rule1",true});
            inputHandler.send(new Object[]{"rule2_dilini",true});
            inputHandler.send(new Object[]{"rule2_tishan",true});
            inputHandler.send(new Object[]{"rule2_suho",true});
            inputHandler.send(new Object[]{"rule2_user1",true});
            inputHandler.send(new Object[]{"rule2_user2",true});
            inputHandler.send(new Object[]{"rule2_user3",true});
            inputHandler.send(new Object[]{"rule2_user4",true});
            inputHandler.send(new Object[]{"rule2_user5",true});
            inputHandler.send(new Object[]{"rule2_user6",true});
            inputHandler.send(new Object[]{"rule2_user7",true});

        } catch (InterruptedException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        eventReceivingServer = new EventReceivingServer();
        eventReceivingServer.start(9611, 9711);

        initDataPublisher();
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
        ResultContainer result = new ResultContainer(ruleCount);
        resultMap.put(uniqueKey.toString(), result);
        getRequestStreamInputHandler().send(new Object[]{uniqueKey, request.getTier(), request.getKey()});
        //Blocked call to return synchronous result
        boolean isThrottled = result.isThrottled();
        if (!isThrottled) { //Only send served request to global throttler
            sendToGlobalThrottler(new Object[]{uniqueKey, request.getTier(), request.getKey()});
        }
        resultMap.remove(uniqueKey);
        return isThrottled;
    }

    public void stop() {
        if (siddhiManager != null) {
            siddhiManager.shutdown();
        }
        if (eventReceivingServer != null) {
            eventReceivingServer.stop();
        }
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
