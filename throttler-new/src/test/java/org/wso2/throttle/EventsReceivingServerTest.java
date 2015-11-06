package org.wso2.throttle;

import org.junit.Test;
import org.wso2.carbon.databridge.core.exception.DataBridgeException;
import org.wso2.carbon.databridge.core.exception.StreamDefinitionStoreException;
import org.wso2.throttle.core.EventReceivingServer;

import java.io.IOException;

public class EventsReceivingServerTest {

    @Test
    public void testServerStartup() throws DataBridgeException, StreamDefinitionStoreException, IOException, InterruptedException {
        EventReceivingServer server = new EventReceivingServer();
        server.start(8611, 8711);
        Thread.sleep(2000);
    }
}
