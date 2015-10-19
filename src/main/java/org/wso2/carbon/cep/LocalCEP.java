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

import org.wso2.siddhi.core.ExecutionPlanRuntime;
import org.wso2.siddhi.core.SiddhiManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class LocalCEP {

    private SiddhiManager siddhiManager;
    private ExecutionPlanRuntime executionPlanRuntime;
    private List<String> queryList = new ArrayList<String>();
    private List<String> definitionList = new ArrayList<String>();

    protected void addThrottlingType(ThrottlingManager.ThrottlingType type, Properties propertyList) {
        if (type == ThrottlingManager.ThrottlingType.Rule1) {
            String apiName = propertyList.getProperty("name");
            definitionList.add("define stream Rule1EvalStream (messageID string, ip string, maxCount int); ");
            definitionList.add("define stream Rule1Stream (ip string, isThrottled bool); ");
            definitionList.add("@IndexedBy('ip') define table Rule1Table (ip string, isThrottled bool); ");

            queryList.add("from Rule1Stream select * insert into Rule1Table; ");
            queryList.add("from Rule1EvalStream [not (Rule1Table.ip == Rule1EvalStream.ip in Rule1Table)] select " +
                    "messageID, ip, false as isThrottled insert into LocalResultStream;");
            queryList.add("from Rule1EvalStream join Rule1Table on Rule1Table.ip == Rule1EvalStream.ip select " +
                    "Rule1EvalStream.messageID, Rule1EvalStream.ip, Rule1Table.isThrottled insert into LocalResultStream;");

        } else {
            definitionList.add("define stream Rule2EvalStream (messageID string, ip string, maxCount int); ");
            definitionList.add("define stream Rule2Stream (ip string, isThrottled bool, throttlingContext string); ");
            definitionList.add("@IndexedBy('ip') define table Rule2Table (ip string, isThrottled bool); ");

            queryList.add("from Rule2Stream select * insert into Rule2Table; ");
            queryList.add("from Rule2EvalStream[not (Rule2Table.ip == Rule2EvalStream.ip in Rule2Table)] select " +
                    "messageID, ip, false as isThrottled insert into LocalResultStream; ");
            queryList.add("from Rule2EvalStream join Rule2Table on Rule2Table.ip == Rule2EvalStream.ip select " +
                    "Rule2EvalStream.messageID , Rule2EvalStream.ip, Rule2Table.isThrottled insert into " +
                    "LocalResultStream; ");
        }
    }

    public void init(){
        definitionList.add("define stream Rule1EvalStream (messageID string, ip string, maxCount int); ");
        definitionList.add("define stream Rule1InStream (ip string, isThrottled bool); ");
        definitionList.add("@IndexedBy('ip') define table Rule1Table (ip string, isThrottled bool); ");

        queryList.add("from Rule1InStream select * insert into Rule1Table; ");
        queryList.add("from Rule1EvalStream [not (Rule1Table.ip == Rule1EvalStream.ip in Rule1Table)] select " +
                "messageID, ip, false as isThrottled insert into LocalResultStream;");
        queryList.add("from Rule1EvalStream join Rule1Table on Rule1Table.ip == Rule1EvalStream.ip select " +
                "Rule1EvalStream.messageID, Rule1EvalStream.ip, Rule1Table.isThrottled insert into LocalResultStream;");


        definitionList.add("define stream Rule2EvalStream (messageID string, ip string, maxCount int); ");
        definitionList.add("define stream Rule2InStream (ip string, isThrottled bool); ");
        definitionList.add("@IndexedBy('ip') define table Rule2Table (ip string, isThrottled bool); ");

        queryList.add("from Rule2InStream select * insert into Rule2Table; ");
        queryList.add("from Rule2EvalStream[not (Rule2Table.ip == Rule2EvalStream.ip in Rule2Table)] select " +
                "messageID, ip, false as isThrottled insert into LocalResultStream; ");
        queryList.add("from Rule2EvalStream join Rule2Table on Rule2Table.ip == Rule2EvalStream.ip select " +
                "Rule2EvalStream.messageID , Rule2EvalStream.ip, Rule2Table.isThrottled insert into " +
                "LocalResultStream; ");
        String fullQuery = constructFullQuery();

        siddhiManager = new SiddhiManager();
        executionPlanRuntime = siddhiManager.createExecutionPlanRuntime(fullQuery);
        executionPlanRuntime.start();

    }

    public ExecutionPlanRuntime getExecutionPlanRuntime() {
        return executionPlanRuntime;
    }

    private String constructFullQuery() {
        StringBuilder stringBuilder = new StringBuilder();
        for(String definition : definitionList){
            stringBuilder.append(definition);
        }
        for(String query : queryList){
            stringBuilder.append(query);
        }

        return stringBuilder.toString();
    }

    /**
     * Clean up method
     */
    public void shutdown() {
        siddhiManager.shutdown();
        queryList = new ArrayList<String>();
        definitionList = new ArrayList<String>();
    }

}
