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

import java.util.HashMap;
import java.util.Map;

public class TemplateStore {
    private static final Logger log = Logger.getLogger(TemplateStore.class);
    private static TemplateStore templateStore;
    private Map<String, String> templateIDToQuery = new HashMap<String, String>();

    public static synchronized TemplateStore getInstance(){
        if(templateStore == null){
            templateStore = new TemplateStore();
        }
        return templateStore;
    }

    private TemplateStore(){
        //Populate templateIDToQuery map with default templates, rule1 and rule2
        templateIDToQuery.put("rule1", "from RequestStream[apiName==$param1 and userID==$param2]\n" +
                                       "select \"rule1\" as rule, \"\" as v1, \"\" as v2, messageID\n" +
                                       "insert into RuleStream;");

        templateIDToQuery.put("rule2", "from RequestStream\n" +
                                       "select \"rule2\" as rule, userID as v1, \"\" as v2, messageID\n" +
                                       "insert into RuleStream;");
    }

    /**
     * To add a new rule-template, specify a unique ID and the parameterized query
     * @param templateID    a unique ID to be given to rule-template
     * @param query         parameterized query
     */
    public void addTemplate(String templateID, String query){
        if(templateIDToQuery.containsKey(templateID)){
            log.warn("Replacing query for template ID: " + templateID + ". Existing query: " + templateIDToQuery.get(templateID)
                     + ". New query: " + query);
        }
        templateIDToQuery.put(templateID, query);
    }

    public String getQueryTemplate(String templateID){
        if(templateIDToQuery.isEmpty()){
            return null;
        }
        return templateIDToQuery.get(templateID);
    }
}
