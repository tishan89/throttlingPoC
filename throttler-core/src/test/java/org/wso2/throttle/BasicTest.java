package org.wso2.throttle;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.wso2.carbon.databridge.core.exception.DataBridgeException;
import org.wso2.carbon.databridge.core.exception.StreamDefinitionStoreException;
import org.wso2.throttle.core.Request;
import org.wso2.throttle.core.Throttler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class BasicTest {
    private static final Logger log = Logger.getLogger(BasicTest.class);

    @Test
    public void testRule1()
            throws InterruptedException, DataBridgeException, StreamDefinitionStoreException,
                   IOException {
        Throttler throttler = Throttler.getInstance();
        throttler.start();

        throttler.addRule("bronze", null, null);
        throttler.addRule("silver", null, null);
        throttler.addRule("gold", null, null);
        long startTime = System.nanoTime();
        Request request;
        for (int i = 0; i < 1; i++) {
            request = new Request("gold","2", "gold","test:1.0.0:abcd:admin@carbon.super", "silver",
                    "test:GET-abcd:admin@carbon.super");
            throttler.isThrottled(request);
        }
        long endTime = System.nanoTime();
        log.info(endTime - startTime);

        Thread.sleep(10000);
        throttler.stop();
    }

    @Test
    public void testPerformance()
            throws InterruptedException, DataBridgeException, StreamDefinitionStoreException,
            IOException {
        int numOfThreads = 30;
        long numTasks = 800000;
        final Throttler throttler = Throttler.getInstance();
        final Request request = new Request("gold","2", "gold","test:1.0.0:abcd:admin@carbon.super", "silver",
                "test:GET-abcd:admin@carbon.super");
        ThrottlingTask task = new ThrottlingTask(throttler, request);
        ExecutorService executorService = Executors.newFixedThreadPool(numOfThreads);

        throttler.start();
        throttler.addRule("bronze", null, null);
        throttler.addRule("silver", null, null);
        throttler.addRule("gold", null, null);

        long startTimeMillis = System.currentTimeMillis();
        for (int i = 0; i < numTasks; i++) {
            executorService.submit(task);
        }

        executorService.shutdown();
        executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);

        long diff = System.currentTimeMillis() - startTimeMillis;
        log.info("Throughput " + (numTasks * 1000 / diff));
        log.info("Latency " + (diff * 1.0 / numTasks + " ms"));
        log.info("Time in milli sec " + diff);
    }

    @Test
    public void testLatency() throws DataBridgeException, StreamDefinitionStoreException, IOException, InterruptedException {
        int numOfThreads = 3;
        int numTasks = 800000;
        int iterations = 10000;
        List<Long> resultList = new ArrayList<Long>(numTasks);
        final Request request = new Request("gold","2", "gold","test:1.0.0:abcd:admin@carbon.super", "silver",
                "test:GET-abcd:admin@carbon.super");
        final Throttler throttler = Throttler.getInstance();
        ThrottlingTask task = new ThrottlingTask(throttler, request);
        ExecutorService executorService = Executors.newFixedThreadPool(numOfThreads);

        throttler.start();
        throttler.addRule("bronze", null, null);
        throttler.addRule("silver", null, null);
        throttler.addRule("gold", null, null);

        //flood the system
        for (int i = 0; i < numTasks; i++) {
            executorService.submit(task);
        }

        for (int j = 0; j < iterations; j++) {
            long startTime = System.nanoTime();
            throttler.isThrottled(request);
            resultList.add(System.nanoTime() - startTime);
        }

        executorService.shutdown();
        Collections.sort(resultList);
        double aggregateLatency = 0.0;
        for (Long latency : resultList) {
            aggregateLatency = +latency;
        }
        log.info("Avg Latency(ns) : " + aggregateLatency / iterations);
        log.info("Max Latency(ns) : " + resultList.get(resultList.size() - 1));
        log.info("Min Latency(ns) : " + resultList.get(0));
    }

    class ThrottlingTask implements Runnable {
        Throttler throttler = null;
        Request request = null;

        ThrottlingTask(Throttler throttler, Request request) {
            this.throttler = throttler;
            this.request = request;
        }

        @Override
        public void run() {
            try {
                throttler.isThrottled(request);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
