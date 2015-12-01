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

/**
 * Request which is sent to Throttler.
 */
public class Request {
    private String appTier;
    private String appKey;
    private String apiTier;
    private String apiKey;
    private String resourceKey;
    private String resourceTier;

    public Request(String appTier, String appKey, String apiTier, String apiKey, String resourceKey, String resourceTier){
        this.appTier = appTier;
        this.appKey = appKey;
        this.apiTier = apiTier;
        this.apiKey = apiKey;
        this.resourceTier = resourceTier;
        this.resourceKey = resourceKey;
    }

    public String getAppTier() {
        return appTier;
    }

    public void setAppTier(String appTier) {
        this.appTier = appTier;
    }

    public String getAppKey() {
        return appKey;
    }

    public void setAppKey(String apiName) {
        this.appKey = apiName;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiTier() {
        return apiTier;
    }

    public void setApiTier(String apiTier) {
        this.apiTier = apiTier;
    }

    public String getResourceKey() {
        return resourceKey;
    }

    public void setResourceKey(String resourceKey) {
        this.resourceKey = resourceKey;
    }

    public String getResourceTier() {
        return resourceTier;
    }

    public void setResourceTier(String resourceTier) {
        this.resourceTier = resourceTier;
    }
}
