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

public class QueryTemplateStore {

//    private static String eligibilityQueryTemplate = "" +
//            "FROM RequestStream\n" +
//            "SELECT \"$LEVEL_$TIER\" AS rule, messageID, ($LEVEL_tier==\"$TIER\") AS isEligible, $LEVEL_key AS key \n" +
//            "INSERT INTO EligibilityStream;";
//
//    public static String constructEligibilityQuery(String level, String tier) {
//        StringBuilder builder = new StringBuilder();
//        String query = eligibilityQueryTemplate.replaceAll("$LEVEL", level.toLowerCase());
//        query = query.replaceAll("$TIER", tier.toLowerCase());
//        builder.append(query);
//        builder.append("\n");
//        return builder.toString();
//    }

    public static String loadThrottlingAttributes() {
        return "messageID string, app_key string, api_key string, resource_key string, app_tier string, api_tier string, resource_tier string, verb string, ip_range string";
    }

    public static String[] loadThrottlingEligibilityQueries() {
        return new String[]{
                "FROM RequestStream\n" +
                        "SELECT 'app_gold' AS rule, messageID, (( not(app_key is null ) AND api_tier=='gold' )  AS isEligible, concat('app_gold_',app_key,'_key') AS key, verb, ip_range\n" +
                        "INSERT INTO EligibilityStream;\n",

                "FROM RequestStream\n" +
                        "SELECT 'app' AS rule, messageID, true AS isEligible, concat('app_',app_key,'_key') AS key, verb, ip_range\n" +
                        "INSERT INTO EligibilityStream;\n"};
    }
}
