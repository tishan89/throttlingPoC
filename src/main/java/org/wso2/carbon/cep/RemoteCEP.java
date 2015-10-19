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

public class RemoteCEP {
    private SiddhiManager siddhiManager = new SiddhiManager();
    private ExecutionPlanRuntime executionPlanRuntime;
    private List<String> queryList = new ArrayList<String>();
    private List<String> definitionList = new ArrayList<String>();
    private List<String> APINameList = new ArrayList<String>();

    protected void addThrottlingType(ThrottlingManager.ThrottlingType type, Properties propertyList) {
        String apiName = propertyList.getProperty("name");
        if (!APINameList.contains(apiName)) {
            definitionList.add("define stream " + apiName + "InStream (ip string, maxCount int); ");
            APINameList.add(apiName);
        }
        if (type == ThrottlingManager.ThrottlingType.Rule1) {

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
    }

    public void init() {
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
        String fullQuery = constructFullQuery();
        executionPlanRuntime = siddhiManager.createExecutionPlanRuntime(fullQuery);
        executionPlanRuntime.start();
    }

    public ExecutionPlanRuntime getExecutionPlanRuntime() {
        return executionPlanRuntime;
    }

    private String constructFullQuery() {
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
