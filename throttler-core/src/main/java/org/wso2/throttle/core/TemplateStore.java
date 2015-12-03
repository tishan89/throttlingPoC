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

import org.wso2.throttle.api.ThrottleLevel;

public class TemplateStore {
    private static String eligibilityQueryTemplate = "FROM RequestStream\n" +
                                 "SELECT \"$THROTTLELEVEL_$TIER\" AS rule, messageID, (not ($THROTTLELEVEL_tier is null) AND $THROTTLELEVEL_tier==\"$TIER\") AS isEligible, " +
                                 "$THROTTLELEVEL_key AS key\n" +
                                 "INSERT INTO EligibilityStream;";

    private static String getEligibilityQueryTemplate() {
        return eligibilityQueryTemplate;
    }

    //todo: improve validation
    public static String getEligibilityQueries(String tier) {
        String eligibilityQueryTemplate = getEligibilityQueryTemplate();

        StringBuilder builder = new StringBuilder();
        for (ThrottleLevel level : ThrottleLevel.values()) {
            String query = eligibilityQueryTemplate.replace("$THROTTLELEVEL", level.toString().toLowerCase());
            query = query.replace("$TIER", tier);
            builder.append(query);
            builder.append("\n");
        }
        return builder.toString();
    }

    public static String getEnforcementQuery() {
        //todo: implement
        return "";
    }
}
