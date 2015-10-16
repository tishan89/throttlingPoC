package org.wso2.carbon.cep;

import junit.framework.Assert;
import org.junit.Test;

import java.util.Properties;

public class testCEP {

    @Test
    public void testLocalCEP(){
        LocalCEP localCEP = new LocalCEP();
        Properties properties = new Properties();
        properties.put("name","API1");
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

    @Test
    public void testSynchronousBehaviour() throws InterruptedException {
        final ResultContainer result = new ResultContainer(2);
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Assert.assertTrue("Assert the throttling result", result.isThrottled());
            }
        });
        thread.start();
        Thread.sleep(1000);
        result.addResult(true);
        result.addResult(false);
    }
}
