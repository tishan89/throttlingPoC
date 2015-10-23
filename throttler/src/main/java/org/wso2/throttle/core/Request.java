package org.wso2.throttle.core;

/**
 * Request which is sent to Throttler.
 */
public class Request {
    private String ip;
    private String apiName;

    public Request(String apiName, String ip){
        this.apiName = apiName;
        this.ip = ip;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getAPIName() {
        return apiName;
    }

    public void setAPIName(String apiName) {
        this.apiName = apiName;
    }
}
