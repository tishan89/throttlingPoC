package org.wso2.throttle;

import org.junit.Test;
import org.wso2.carbon.databridge.core.exception.DataBridgeException;
import org.wso2.carbon.databridge.core.exception.StreamDefinitionStoreException;
import org.wso2.throttle.core.Request;
import org.wso2.throttle.core.Throttler;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class BasicTest {
    @Test
    public void testRule1() throws InterruptedException, DataBridgeException, StreamDefinitionStoreException, IOException {
        Throttler throttler = Throttler.getInstance();
        throttler.start();

        throttler.addRule("rule1", "api1", "dilini");
        throttler.addRule("rule2", null, null);

        for(int i=0; i<10; i++) {
            System.out.println(throttler.isThrottled(new Request("api1", "dilini")));
        }

        Thread.sleep(10000);
        throttler.stop();
    }

    @Test
    public void testPerformance() throws InterruptedException, DataBridgeException, StreamDefinitionStoreException,
            IOException {
        int numOfThreads = 20;
        final Throttler throttler = Throttler.getInstance();
        throttler.start();
        throttler.addRule("rule1", "api1", "dilini");
        throttler.addRule("rule2", null, null);
        final Request request = new Request("api1", "dilini");
        ExecutorService executorService = Executors.newFixedThreadPool(numOfThreads);

        Runnable testRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    throttler.isThrottled(request);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }


            }
        };

        long numIterations = 20000;
        long startTimeMillis = System.currentTimeMillis();
        for (int i = 0; i < numIterations; i++) {
            executorService.submit(testRunnable);
        }
        executorService.shutdown();
        executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        long diff = System.currentTimeMillis() - startTimeMillis;
        System.out.println("Throughput " + (numIterations * 1000 / diff));
        System.out.println("Time in sec" + diff / 1000);
    }
}
