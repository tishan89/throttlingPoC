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

public class GlobalTemplateStore {
//    private static final Logger log = Logger.getLogger(GlobalTemplateStore.class);
//    private static GlobalTemplateStore templateStore;
//    private Map<String, String> templateIDToQuery = new HashMap<String, String>();
//
//    public static synchronized GlobalTemplateStore getInstance(){
//        if(templateStore == null){
//            templateStore = new GlobalTemplateStore();
//        }
//        return templateStore;
//    }
//
//    private GlobalTemplateStore(){
//        //Populate templateIDToQuery map with default templates, rule1 and rule2
//        templateIDToQuery.put("rule1", "from RequestStream[apiName==$param1 and userID==$param2]\n" +
//                                       "select \"rule1\" as rule, \"\" as v1, \"\" as v2, messageID\n" +
//                                       "insert into RuleStream;" +
//                                       "\n" +
//                                       "from RuleStream[rule==\"rule1\"]#window.time(60000) \n" +
//                                       "select str:concat(\"rule1_\",RuleStream.v1,\"_\",RuleStream.v2) as key, (count(messageID) >= 5) as isThrottled \n" +
//                                       "insert all events into IntermediateResultStream;");
//
//        templateIDToQuery.put("rule2", "from RequestStream\n" +
//                                       "select \"rule2\" as rule, userID as v1, \"\" as v2, messageID\n" +
//                                       "insert into RuleStream;" +
//                                       "\n" +
//                                       "from RuleStream[rule==\"rule2\"]\n" +
//                                       "select *\n" +
//                                       "insert into Rule2Stream;\n" +
//                                       "\n" +
//                                       "partition with (v1 of Rule2Stream)\n" +
//                                       "begin\n" +
//                                       "from Rule2Stream#window.time(60000) \n" +
//                                       "select str:concat(\"rule2_\",RuleStream.v1,\"_\",RuleStream.v2) as key, (count(messageID) >= 10) as isThrottled \n" +
//                                       "insert all events into IntermediateResultStream;\n" +
//                                       "end;");
//    }
//
//    /**
//     * To add a new rule-template, specify a unique ID and the parameterized query
//     * @param templateID    a unique ID to be given to rule-template
//     * @param query         parameterized query
//     */
//    public void addTemplate(String templateID, String query){
//        if(templateIDToQuery.containsKey(templateID)){
//            log.warn("Replacing query for template ID: " + templateID + ". Existing query: " + templateIDToQuery.get(templateID)
//                     + ". New query: " + query);
//        }
//        templateIDToQuery.put(templateID, query);
//    }
//
//    public String getQueryTemplate(String templateID){
//        if(templateIDToQuery.isEmpty()){
//            return null;
//        }
//        return templateIDToQuery.get(templateID);
//    }
}
