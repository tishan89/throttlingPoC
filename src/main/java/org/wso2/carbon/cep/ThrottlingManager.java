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
import java.util.List;
import java.util.Properties;

public class ThrottlingManager {

    private static final Logger log = Logger.getLogger(ThrottlingManager.class);

    public enum ThrottlingType {
        Rule1, Rule2
    }
    //Rule1 = 10 request per each user for duration of 5 min regardless of API
    //Rule2 = 5 req per each user for duration of 5min for API1

    private static List<LocalCEP> localCEPList = new ArrayList<LocalCEP>();
    private static RemoteCEP remoteCEP = new RemoteCEP();
    private static List<ExecutionPlanRuntime> localRuntimes = new ArrayList<ExecutionPlanRuntime>();

    private ThrottlingManager() {
        //initialization stopped
    }

    public static void addLocalCEP(int number) {
        for (int i = 0; i < number; i++) {
            localCEPList.add(new LocalCEP());
        }
    }

    public static void addThrottling(ThrottlingType type, Properties propertyList) {
        for (LocalCEP localCEP : localCEPList) {
            localCEP.addThrottlingType(type, propertyList);
        }
        remoteCEP.addThrottlingType(type, propertyList);
    }

    public static void init() {
        for (LocalCEP localCEP : localCEPList) {
            localCEP.init();
            localRuntimes.add(localCEP.getExecutionPlanRuntime());
        }
        remoteCEP.init();

        //Wiring remote node's output to local CEP
        remoteCEP.getExecutionPlanRuntime().addCallback("remoteOutStream", new StreamCallback() {
            @Override
            public void receive(Event[] events) {
                EventPrinter.print(events);
                for (Event event : events) {
                    String throttlingContext = (String) event.getData(2);
                    for (ExecutionPlanRuntime runtime : localRuntimes) {
                        InputHandler inputHandler = runtime.getInputHandler(throttlingContext + "InStream");
                        try {
                            inputHandler.send(event);
                        } catch (InterruptedException e) {
                            log.error("Event sending failed", e);
                        }
                    }
                }
            }
        });
    }

    public static List<LocalCEP> getLocalCEPList() {
        return localCEPList;
    }

    public static RemoteCEP getRemoteCEP() {
        return remoteCEP;
    }
}
