package org.wso2.carbon.cep;

import org.junit.Test;

import java.util.Properties;

public class testCEP {

    @Test
    public void testLocalCEP(){
        LocalCEP localCEP = new LocalCEP();
        Properties properties = new Properties();
        properties.put("name","API1");
        localCEP.addThrottlingType(ThrottlingManager.ThrottlingType.Rule1, properties);
        localCEP.addThrottlingType(ThrottlingManager.ThrottlingType.Rule2, properties);
        localCEP.init();
    }

    @Test
    public void testRemoteCEP() {
        RemoteCEP remoteCEP = new RemoteCEP();
        Properties properties = new Properties();
        properties.put("name", "API1");
        remoteCEP.addThrottlingType(ThrottlingManager.ThrottlingType.Rule1, properties);
        remoteCEP.addThrottlingType(ThrottlingManager.ThrottlingType.Rule2, properties);
        remoteCEP.init();
    }
}
