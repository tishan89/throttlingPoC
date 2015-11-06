package org.wso2.throttle;

import org.junit.Test;
import org.wso2.carbon.databridge.core.exception.DataBridgeException;
import org.wso2.carbon.databridge.core.exception.StreamDefinitionStoreException;
import org.wso2.throttle.core.Request;
import org.wso2.throttle.core.Throttler;

import java.io.IOException;

public class BasicTest {
    @Test
    public void testRule1() throws InterruptedException, DataBridgeException, StreamDefinitionStoreException, IOException {
        Throttler throttler = Throttler.getInstance();
        throttler.start();

        throttler.addRule("rule1", "api1", "dilini");
        throttler.addRule("rule2", null, null);

        throttler.isThrottled(new Request("api1", "dilini"));
        throttler.isThrottled(new Request("api1", "dilini"));
        throttler.isThrottled(new Request("api1", "dilini"));
        throttler.isThrottled(new Request("api1", "dilini"));
        throttler.isThrottled(new Request("api1", "dilini"));

        Thread.sleep(10000);
        throttler.stop();
    }
}
