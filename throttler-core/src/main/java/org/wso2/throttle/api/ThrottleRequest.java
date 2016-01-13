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

package org.wso2.throttle.api;

import java.util.Map;

/**
 * ThrottleRequest which is sent to {@link org.wso2.throttle.core.Throttler} to request throttling decision.
 */
public class ThrottleRequest {

    private String throttleKey1;
    private String throttleKey2;
    private String throttleKey3;
    private String[] throttleTier1;
    private String[] throttleTier2;
    private String[] throttleTier3;
    private Map<String, String> properties;


    /**
     * Construct request object for {@link org.wso2.throttle.core.Throttler}
     *
     * @param throttleKey1  Level 1 Throttling key  (should be unique)
     * @param throttleKey2  Level 2 Throttling key  (should be unique)
     * @param throttleKey3  Level 3 Throttling key  (should be unique)
     * @param throttleTier1 Level 1 Throttling Tier
     * @param throttleTier2 Level 1 Throttling Tier
     * @param throttleTier3 Level 1 Throttling Tier
     * @param properties    other throttling properties
     */
    public ThrottleRequest(String throttleKey1, String throttleKey2, String throttleKey3, String[] throttleTier1, String[] throttleTier2, String[] throttleTier3, Map<String, String> properties) {
        this.throttleKey1 = throttleKey1;
        this.throttleKey2 = throttleKey2;
        this.throttleKey3 = throttleKey3;
        this.throttleTier1 = throttleTier1;
        this.throttleTier2 = throttleTier2;
        this.throttleTier3 = throttleTier3;
        this.properties = properties;
    }


    public String getThrottleKey1() {
        return throttleKey1;
    }

    public String getThrottleKey2() {
        return throttleKey2;
    }

    public String getThrottleKey3() {
        return throttleKey3;
    }

    public String[] getThrottleTier1() {
        return throttleTier1;
    }

    public String[] getThrottleTier2() {
        return throttleTier2;
    }

    public String[] getThrottleTier3() {
        return throttleTier3;
    }

    public Map<String, String> getProperties() {
        return properties;
    }
}
