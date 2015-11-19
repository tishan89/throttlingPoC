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
import java.util.concurrent.atomic.AtomicLong;

public class BasicTest {
    private AtomicLong requestsCompleted = new AtomicLong(0);
    private static AtomicLong totalDelay = new AtomicLong(0);
    private AtomicLong totalLatency = new AtomicLong(0);
    private AtomicLong numBatches = new AtomicLong(0);

    @Test
    public void testRule1() throws InterruptedException, DataBridgeException, StreamDefinitionStoreException, IOException {
        Throttler throttler = Throttler.getInstance();
           long starttime = System.nanoTime();
        //for(int i=0; i<1; i++) {
            throttler.isThrottled(new Request("gold", "app1dilini"));
        //}
        long end = System.nanoTime();
        System.out.println(end - starttime);

        Thread.sleep(10000);
        throttler.stop();
    }

    @Test
    public void testPerformance() throws InterruptedException, DataBridgeException, StreamDefinitionStoreException,
            IOException {
        int numOfThreads = 20;
        final Throttler throttler = Throttler.getInstance();
        final Request request = new Request("gold", "app1dilini");
        ExecutorService executorService = Executors.newFixedThreadPool(numOfThreads);

        long numTasks = 900000;   //700000
        long startTimeMillis = System.currentTimeMillis();
        for (int i = 0; i < numTasks; i++) {
            executorService.submit(new TaskSubmitter(throttler, request));
        }
        executorService.shutdown();
        executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        long diff = System.currentTimeMillis() - startTimeMillis;
        System.out.println("Throughput " + (numTasks * 1000 / diff));
        System.out.println("Time in milli sec " + diff );
        System.out.println("Latency " + totalLatency.get() / numBatches.get());
    }

    class TaskSubmitter implements Runnable {
        Throttler throttler = null;
        Request request = null;

        TaskSubmitter(Throttler throttler, Request request){
            this.throttler = throttler;
            this.request = request;
        }

        @Override
        public void run() {
            try {
                long startTime = System.nanoTime();
                throttler.isThrottled(request);
                long end = System.nanoTime();
                requestsCompleted.incrementAndGet();
                totalDelay.addAndGet(end - startTime);
                if(requestsCompleted.get() % 1000 == 0) {
                    totalLatency.addAndGet(totalDelay.get() / 1000);
                    numBatches.incrementAndGet();
                    totalDelay.set(0);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
