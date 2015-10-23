package org.wso2.carbon.cep;

import junit.framework.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.util.Properties;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class testCEP {

//    @Test
//    public void testLocalCEP(){
//        LocalCEP localCEP = new LocalCEP();
//        Properties properties = new Properties();
//        properties.put("name","API1");
//        localCEP.init();
//    }
//
//    @Test
//    public void testRemoteCEP() {
//        RemoteCEP remoteCEP = new RemoteCEP();
//        Properties properties = new Properties();
//        properties.put("name", "API1");
//        remoteCEP.buildQueryList(ThrottlingManager.ThrottlingRule.Rule1, properties);
//        remoteCEP.buildQueryList(ThrottlingManager.ThrottlingRule.Rule2, properties);
//        remoteCEP.init();
//    }
//
//    @Test
//    public void testSynchronousBehaviour() throws InterruptedException {
//        final ResultContainer result = new ResultContainer(2);
//        Thread thread = new Thread(new Runnable() {
//            @Override
//            public void run() {
//                Assert.assertTrue("Assert the throttling result", result.isThrottled());
//            }
//        });
//        thread.start();
//        Thread.sleep(1000);
//        result.addResult(true);
//        result.addResult(false);
//    }

    @Test
    public void testRule1() throws InterruptedException {

        Properties api1Properties = new Properties();
        api1Properties.put("name", "API1");
        ThrottlingManager.buildQueries(ThrottlingManager.ThrottlingRule.Rule1, api1Properties);
        ThrottlingManager.buildQueries(ThrottlingManager.ThrottlingRule.Rule2, api1Properties);

        Properties api2Properties = new Properties();
        api2Properties.put("name", "API2");
        ThrottlingManager.buildQueries(ThrottlingManager.ThrottlingRule.Rule2, api2Properties);

        ThrottlingManager.start();

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

        Properties api1Properties = new Properties();
        api1Properties.put("name", "API1");
        ThrottlingManager.buildQueries(ThrottlingManager.ThrottlingRule.Rule1, api1Properties);
        ThrottlingManager.buildQueries(ThrottlingManager.ThrottlingRule.Rule2, api1Properties);

        Properties api2Properties = new Properties();
        api2Properties.put("name", "API2");
        ThrottlingManager.buildQueries(ThrottlingManager.ThrottlingRule.Rule2, api2Properties);

        ThrottlingManager.start();

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
