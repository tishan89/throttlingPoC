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

/**
 * Request which is sent to {@link org.wso2.throttle.core.Throttler} to request throttling decision.
 */
public class Request {
    private String appKey;
    private String apiKey;
    private String resourceKey;

    private String appTier;
    private String apiTier;
    private String resourceTier;

    /**
     * Construct request object for {@link org.wso2.throttle.core.Throttler}
     * @param appKey Throttling key for application level (should be unique)
     * @param apiKey Throttling key for API level (should be unique)
     * @param resourceKey Throttling key for resource level (should be unique)
     * @param appTier Throttling Tier for application level
     * @param apiTier Throttling Tier for API level
     * @param resourceTier Throttling Tier for resource level
     */
    public Request(String appKey, String apiKey, String resourceKey, String appTier, String apiTier, String resourceTier) {
        this.appKey = appKey;
        this.apiKey = apiKey;
        this.resourceKey = resourceKey;
        this.appTier = appTier;
        this.apiTier = apiTier;
        this.resourceTier = resourceTier;
    }

    public String getAppKey() {
        return appKey;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getResourceKey() {
        return resourceKey;
    }

    public String getAppTier() {
        return appTier;
    }

    public String getApiTier() {
        return apiTier;
    }

    public String getResourceTier() {
        return resourceTier;
    }
}
