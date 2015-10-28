/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.throttle.global.core;

import org.apache.log4j.Logger;
import org.wso2.carbon.databridge.agent.AgentHolder;
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
import org.wso2.siddhi.core.stream.output.StreamCallback;
import org.wso2.siddhi.core.util.EventPrinter;
import org.wso2.throttle.common.util.DataPublisherTestUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Throttler {
    private static final Logger log = Logger.getLogger(Throttler.class);

    private Throttler throttler;
    private SiddhiManager siddhiManager = new SiddhiManager();


    private Map<String, ExecutionPlanRuntime> apiToEPRuntimeMap = new HashMap<String, ExecutionPlanRuntime>();
    private Map<String, List<Policy>> apiToPoliciesMap = new HashMap<String, List<Policy>>();

    private Throttler() {
    }

    public Throttler getInstance() {
        if (throttler == null) {
            throttler = new Throttler();
        }
        return throttler;
    }

    public void init() {

    }

    public void start() {
    }

    private void addCallbacks(ExecutionPlanRuntime runtime) {
        runtime.addCallback("remoteOutStream", new StreamCallback() {

            @Override
            public void receive(Event[] events) {
                EventPrinter.print(events);
                for (Event event : events) {
                    String throttlingRule = (String) event.getData(2);
                    sendToLocalThrottler(event, throttlingRule);
//                    InputHandler inputHandler = localCEP.getExecutionPlanRuntime().getInputHandler(throttlingRule + "InStream");
//                    try {
//                        inputHandler.send(new Object[]{event.getData(0), event.getData(1)});
//                    } catch (InterruptedException e) {
//                        log.error("Event sending failed", e);
//                    }
                }
            }

            private void sendToLocalThrottler(Event siddhiEvent, String throttlingRule) {
                AgentHolder.setConfigPath(DataPublisherTestUtil.getDataAgentConfigPath());
                String hostName = DataPublisherTestUtil.LOCAL_HOST;
                DataPublisher dataPublisher = null;
                try {
                    dataPublisher = new DataPublisher("Binary", "tcp://" + hostName + ":9681",
                            "ssl://" + hostName + ":9781", "admin", "admin");
                } catch (DataEndpointAgentConfigurationException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                } catch (DataEndpointException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                } catch (DataEndpointConfigurationException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                } catch (DataEndpointAuthenticationException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                } catch (TransportException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
                org.wso2.carbon.databridge.commons.Event databridgeEvent = new org.wso2.carbon.databridge.commons.Event();
                databridgeEvent.setStreamId(DataBridgeCommonsUtils.generateStreamId(throttlingRule + "InStream", ""));
                databridgeEvent.setMetaData(null);
                databridgeEvent.setCorrelationData(null);
                databridgeEvent.setPayloadData(new Object[]{siddhiEvent.getData(0), siddhiEvent.getData(1)});

                dataPublisher.publish(databridgeEvent);
            }

        });
    }

    public synchronized void addPolicy(String apiName, Policy policy) {         //todo: need removePolicy() also
        String newExecutionPlan = createExecutionPlan(policy, apiName);
        ExecutionPlanRuntime newExecutionPlanRuntime = siddhiManager.createExecutionPlanRuntime(newExecutionPlan);
        ExecutionPlanRuntime oldExecutionPlanRuntime = apiToEPRuntimeMap.get(apiName);
        if (oldExecutionPlanRuntime != null) {
            //todo: add debug log on shutting down old runtime
            oldExecutionPlanRuntime.shutdown();
        }
        //todo: add debug log on starting new runtime
        newExecutionPlanRuntime.start();
        addCallbacks(newExecutionPlanRuntime);
        apiToEPRuntimeMap.put(apiName, newExecutionPlanRuntime);

        //Update apiToPoliciesMap
        List<Policy> policyList = apiToPoliciesMap.get(apiName);
        if (policyList == null) {
            policyList = new ArrayList<Policy>();
        }
        policyList.add(policy);
        apiToPoliciesMap.put(apiName, policyList);
    }

    private String createExecutionPlan(Policy policy, String apiName) {
        List<String> queryList = new ArrayList<String>();
        List<String> definitionList = new ArrayList<String>();

        if (policy == Policy.POLICY1) {

            queryList.add("@info(name = 'remoteQuery1')\n" +
                    "partition with (ip of " + apiName + "InStream)\n" +
                    "begin \n" +
                    "\n" +
                    "from " + apiName + "InStream#window.time(5000) \n" +
                    "select ip , (count(ip) >= maxCount) as isThrottled \n" +
                    "insert all events into #outputStream;\n" +
                    "\n" +
                    "from every e1=#outputStream, e2=#outputStream[(e1.isThrottled != e2.isThrottled)] \n" +
                    "select e1.ip, e2.isThrottled, 'Rule1' as throttlingLevel insert into remoteOutStream;\n" +
                    "\n" +
                    "from e1=#outputStream \n" +
                    "select e1.ip, e1.isThrottled, 'Rule1' as throttlingLevel\n" +
                    "insert into remoteOutStream;\n" +
                    "\n" +
                    "end;");

        } else {
            //definitionList.add("define stream GlobalInStream (ip string, maxCount int); ");
            queryList.add("from " + apiName + "InStream select * insert into GlobalInStream;");
        }

        definitionList.add("define stream GlobalInStream (ip string, maxCount int); ");
        queryList.add("@info(name = 'remoteQuery2')\n" +
                "partition with (ip of GlobalInStream)\n" +
                "begin \n" +
                "\n" +
                "from GlobalInStream#window.time(5000) \n" +
                "select ip , (count(ip) >= maxCount) as isThrottled \n" +
                "insert all events into #outputStream;\n" +
                "\n" +
                "from every e1=#outputStream, e2=#outputStream[(e1.isThrottled != e2.isThrottled)] \n" +
                "select e1.ip, e2.isThrottled, 'Rule2' as throttlingLevel insert into remoteOutStream;\n" +
                "\n" +
                "from e1=#outputStream \n" +
                "select e1.ip, e1.isThrottled, 'Rule2' as throttlingLevel\n" +
                "insert into remoteOutStream;\n" +
                "\n" +
                "end; ");

        return constructFullQuery(definitionList, queryList);
    }

    private String constructFullQuery(List<String> definitionList, List<String> queryList) {
        StringBuilder stringBuilder = new StringBuilder();
        for (String definition : definitionList) {
            stringBuilder.append(definition);
        }
        for (String query : queryList) {
            stringBuilder.append(query);
        }

        return stringBuilder.toString();
    }
}
