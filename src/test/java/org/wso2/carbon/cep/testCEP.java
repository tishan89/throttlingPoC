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

    @Test
    public void testRule1() throws InterruptedException {
        Properties API1Properties = new Properties();
        API1Properties.put("name", "API1");
        Properties API2Properties = new Properties();
        API2Properties.put("name", "API2");
        ThrottlingManager.addThrottling(ThrottlingManager.ThrottlingType.Rule1, API1Properties);
        ThrottlingManager.addThrottling(ThrottlingManager.ThrottlingType.Rule2, API1Properties);
        ThrottlingManager.addThrottling(ThrottlingManager.ThrottlingType.Rule2, API2Properties);

        ThrottlingManager.init();

        ThrottlingManager.isThrottled(new Request("API1", "10.100.5.99"));
        ThrottlingManager.isThrottled(new Request("API1", "10.100.5.99"));
        ThrottlingManager.isThrottled(new Request("API1", "10.100.5.99"));
        ThrottlingManager.isThrottled(new Request("API1", "10.100.5.99"));
        ThrottlingManager.isThrottled(new Request("API1", "10.100.5.99"));
        Thread.sleep(100);
        Assert.assertTrue("Assert the throttling result", ThrottlingManager.isThrottled(new Request("API1",
                "10.100.5.99")));
    }

    @Test
    public void testRule2() throws InterruptedException {
        Properties API1Properties = new Properties();
        API1Properties.put("name", "API1");
        Properties API2Properties = new Properties();
        API2Properties.put("name", "API2");
        ThrottlingManager.addThrottling(ThrottlingManager.ThrottlingType.Rule1, API1Properties);
        ThrottlingManager.addThrottling(ThrottlingManager.ThrottlingType.Rule2, API1Properties);
        ThrottlingManager.addThrottling(ThrottlingManager.ThrottlingType.Rule2, API2Properties);

        ThrottlingManager.init();

        ThrottlingManager.isThrottled(new Request("API1", "10.100.5.99"));
        ThrottlingManager.isThrottled(new Request("API1", "10.100.5.99"));
        ThrottlingManager.isThrottled(new Request("API1", "10.100.5.99"));
        ThrottlingManager.isThrottled(new Request("API1", "10.100.5.99"));
        ThrottlingManager.isThrottled(new Request("API2", "10.100.5.99"));
        Thread.sleep(100);
        Assert.assertTrue("Assert the throttling result", ThrottlingManager.isThrottled(new Request("API2",
                "10.100.5.99")));
        Assert.assertFalse("Assert the throttling result", ThrottlingManager.isThrottled(new Request("API1",
                "10.100.5.100")));
    }
}
