package org.wso2.throttle;

import org.junit.Assert;
import org.junit.Test;
import org.wso2.throttle.core.Policy;
import org.wso2.throttle.core.Request;
import org.wso2.throttle.core.Throttler;

public class BasicTest {
    @Test
    public void testRule1() throws InterruptedException{
        Throttler throttler = new Throttler();
        throttler.addPolicy("API1", Policy.POLICY1);

        throttler.isThrottled(new Request("API1", "10.100.5.99"));
        throttler.isThrottled(new Request("API1", "10.100.5.99"));
        throttler.isThrottled(new Request("API1", "10.100.5.99"));
        throttler.isThrottled(new Request("API1", "10.100.5.99"));
        throttler.isThrottled(new Request("API1", "10.100.5.99"));

        Thread.sleep(100);
        Assert.assertTrue("Assert the throttling result", throttler.isThrottled(new Request("API1", "10.100.5.99")));
    }
}
