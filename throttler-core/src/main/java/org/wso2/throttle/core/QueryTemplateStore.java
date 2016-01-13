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
    private static String eligibilityQueryTemplate = "" +
            "FROM RequestStream\n" +
            "SELECT \"$RULE_$TIER\" AS rule, messageID, ($RULE_tier==\"$TIER\") AS isEligible, $RULE_key AS key \n" +
            "INSERT INTO EligibilityStream;";

    //todo: improve validation
    public static String constructEligibilityQuery(String rule, String tier) {
        StringBuilder builder = new StringBuilder();
        String query = eligibilityQueryTemplate.replaceAll("$RULE", rule.toLowerCase());
        query = query.replaceAll("$TIER", tier.toLowerCase());
        builder.append(query);
        builder.append("\n");
        return builder.toString();
    }

    public static String constructEnforcementQuery() {
        //todo: implement
        return "";
    }
}
