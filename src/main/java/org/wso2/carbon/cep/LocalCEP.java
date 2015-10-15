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

    private SiddhiManager siddhiManager = new SiddhiManager();
    private ExecutionPlanRuntime executionPlanRuntime;
    private List<String> queryList = new ArrayList<String>();
    private List<String> definitionList = new ArrayList<String>();

    protected void addThrottlingType(ThrottlingManager.ThrottlingType type, Properties propertyList) {
        if (type == ThrottlingManager.ThrottlingType.Rule1) {
            String apiName = propertyList.getProperty("name");
            definitionList.add("define stream "+apiName+"InStream (ip string, maxCount int); ");
            definitionList.add("define stream Rule1Stream (ip string, isThrottled bool, throttlingContext string); ");
            definitionList.add("@IndexedBy('ip') define table Rule1Table (ip string, isThrottled bool); ");

            queryList.add("from Rule1Stream select * insert into Rule1Table; ");
            queryList.add("from "+apiName+"InStream [not (Rule1Table.ip == API1InStream.ip in Rule1Table)] select " +
                    "API1InStream.ip, false as isThrottled insert into Rule1ResultStream; ");
            queryList.add("from "+apiName+"InStream join Rule1Table on Rule1Table.ip == API1InStream.ip select " +
                    "API1InStream.ip, Rule1Table.isThrottled insert into Rule1ResultStream; ");

        } else {
            definitionList.add("define stream GlobalInStream (ip string, maxCount int); ");
            definitionList.add("define stream Rule2Stream (ip string, isThrottled bool, throttlingContext string); ");
            definitionList.add("@IndexedBy('ip') define table Rule2Table (ip string, isThrottled bool); ");

            queryList.add("from Rule2Stream select * insert into Rule2Table; ");
            queryList.add("from GlobalInStream[not (Rule2Table.ip == ip in Rule2Table)] select " +
                    "GlobalInStream.ip, false as isThrottled insert into Rule2ResultStream; ");
            queryList.add("from GlobalInStream join Rule2Table on Rule2Table.ip == GlobalInStream.ip select " +
                    "GlobalInStream.ip, Rule2Table.isThrottled insert into Rule2ResultStream; ");
        }
    }

    public void init(){
        String fullQuery = constructFullQuery();
        executionPlanRuntime = siddhiManager.createExecutionPlanRuntime(fullQuery);

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



}
