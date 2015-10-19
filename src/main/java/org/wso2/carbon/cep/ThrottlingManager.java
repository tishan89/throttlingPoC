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
package org.wso2.carbon.cep;


import org.apache.log4j.Logger;
import org.wso2.siddhi.core.ExecutionPlanRuntime;
import org.wso2.siddhi.core.event.Event;
import org.wso2.siddhi.core.stream.input.InputHandler;
import org.wso2.siddhi.core.stream.output.StreamCallback;
import org.wso2.siddhi.core.util.EventPrinter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ThrottlingManager {

    private static final Logger log = Logger.getLogger(ThrottlingManager.class);

    public enum ThrottlingType {
        Rule1, Rule2
    }
    //Rule2 = 10 request per each user for duration of 5 min regardless of API
    //Rule1 = 5 req per each user for duration of 5min for API1

    private static LocalCEP localCEP = new LocalCEP();
    private static RemoteCEP remoteCEP = new RemoteCEP();
    private static int maxCount = 5;
    private static Map<String, List<ThrottlingType>> APIThrottlingTypeMap = new HashMap<String, List<ThrottlingType>>();

    private static Map<String, ResultContainer> resultMap = new ConcurrentHashMap<String, ResultContainer>();

    private ThrottlingManager() {
        //initialization stopped
    }

    /**
     * Method to check whether a request is throttled synchronously.
     * Yet to optimize
     *
     * @param request API request
     */
    public static synchronized Boolean isThrottled(Request request) {

        String APIName = request.getAPIName();
        ExecutionPlanRuntime localRuntime = localCEP.getExecutionPlanRuntime();
        UUID uniqueKey = UUID.randomUUID();
        ResultContainer result = new ResultContainer(APIThrottlingTypeMap.get(APIName).size());
        resultMap.put(uniqueKey.toString(), result);
        try {
            for (ThrottlingType type : APIThrottlingTypeMap.get(APIName)) {
                localRuntime.getInputHandler(type.name() + "EvalStream").send(new Object[]{uniqueKey.toString(), request.getIP(), maxCount});
            }
        } catch (InterruptedException e) {
            log.error("Error sending events to Siddhi " + e.getMessage(), e);
        }
        Boolean isThrottled = result.isThrottled();
        if (!isThrottled) {
            InputHandler remotePerAPIHandler = remoteCEP.getExecutionPlanRuntime().getInputHandler(APIName + "InStream");
            try {
                remotePerAPIHandler.send(new Object[]{request.getIP(), maxCount});
            } catch (InterruptedException e) {
                log.error("Error sending events to Siddhi " + e.getMessage(), e);
            }
        }
        resultMap.remove(uniqueKey);
        return isThrottled;

    }

    public static void addThrottling(ThrottlingType type, Properties propertyList) {
        //localCEP.addThrottlingType(type, propertyList);
        remoteCEP.addThrottlingType(type, propertyList);
        if (APIThrottlingTypeMap.containsKey(propertyList.getProperty("name"))) {
            APIThrottlingTypeMap.get(propertyList.getProperty("name")).add(type);
        } else {
            List<ThrottlingType> throttlingTypeList = new ArrayList<ThrottlingType>();
            throttlingTypeList.add(type);
            APIThrottlingTypeMap.put(propertyList.getProperty("name"), throttlingTypeList);
        }
    }

    public static void init() {

        localCEP.init();
        remoteCEP.init();

        //Wiring remote node's output to local CEP
        remoteCEP.getExecutionPlanRuntime().addCallback("remoteOutStream", new StreamCallback() {
            @Override
            public void receive(Event[] events) {
                EventPrinter.print(events);
                for (Event event : events) {
                    String throttlingRule = (String) event.getData(2);

                    InputHandler inputHandler = localCEP.getExecutionPlanRuntime().getInputHandler(throttlingRule + "InStream");
                    try {
                        inputHandler.send(new Object[]{event.getData(0), event.getData(1)});
                    } catch (InterruptedException e) {
                        log.error("Event sending failed", e);
                    }
                }
            }
        });

        localCEP.getExecutionPlanRuntime().addCallback("LocalResultStream", new StreamCallback() {
            @Override
            public void receive(Event[] events) {
                for (Event event : events) {
                    resultMap.get(event.getData(0)).addResult((Boolean) event.getData(2));
                }
            }
        });


    }

    public static LocalCEP getLocalCEP() {
        return localCEP;
    }

    public static RemoteCEP getRemoteCEP() {
        return remoteCEP;
    }
}