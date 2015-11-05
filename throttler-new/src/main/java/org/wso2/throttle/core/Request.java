package org.wso2.throttle.core;

/**
 * Request which is sent to Throttler.
 */
public class Request {
    private String userId;
    private String apiName;

    public Request(String apiName, String userId){
        this.apiName = apiName;
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getAPIName() {
        return apiName;
    }

    public void setAPIName(String apiName) {
        this.apiName = apiName;
    }
}
