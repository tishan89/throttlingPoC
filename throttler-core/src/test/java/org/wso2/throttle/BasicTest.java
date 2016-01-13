package org.wso2.throttle;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.wso2.throttle.api.ThrottleRequest;
import org.wso2.throttle.core.Throttler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class BasicTest {
    private static final Logger log = Logger.getLogger(BasicTest.class);

//    @Test
//    public void testRule1() throws InterruptedException {
//        Throttler throttler = Throttler.getInstance();
//
//        throttler.addRule("bronze");
//        throttler.addRule("silver");
//        throttler.addRule("gold");
//        long startTime = System.nanoTime();
//        ThrottleRequest throttleRequest;
//        for (int i = 0; i < 1; i++) {
//            throttleRequest = new ThrottleRequest("2",
//                    "gold");
//            throttler.isThrottled(throttleRequest);
//        }
//        long endTime = System.nanoTime();
//        log.info(endTime - startTime);
//
//        Thread.sleep(10000);
//        throttler.stop();
//    }
//
//    @Test
//    public void testPerformance()
//            throws InterruptedException {
//        int numOfThreads = 30;
//        long numTasks = 800000;
//        final Throttler throttler = Throttler.getInstance();
//        final ThrottleRequest throttleRequest = new ThrottleRequest("2",
//                "gold");
//        ThrottlingTask task = new ThrottlingTask(throttler, throttleRequest);
//        ExecutorService executorService = Executors.newFixedThreadPool(numOfThreads);
//
//        throttler.addRule("bronze");
//        throttler.addRule("silver");
//        throttler.addRule("gold");
//
//        long startTimeMillis = System.currentTimeMillis();
//        for (int i = 0; i < numTasks; i++) {
//            executorService.submit(task);
//        }
//
//        executorService.shutdown();
//        executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
//
//        long diff = System.currentTimeMillis() - startTimeMillis;
//        log.info("Throughput " + (numTasks * 1000 / diff));
//        log.info("Latency " + (diff * 1.0 / numTasks + " ms"));
//        log.info("Time in milli sec " + diff);
//    }
//
//    @Test
//    public void testLatency() throws InterruptedException {
//        int numOfThreads = 3;
//        int numTasks = 800000;
//        int iterations = 10000;
//        List<Long> resultList = new ArrayList<Long>(numTasks);
//        final ThrottleRequest throttleRequest = new ThrottleRequest("2",
//                "gold");
//        final Throttler throttler = Throttler.getInstance();
//        ThrottlingTask task = new ThrottlingTask(throttler, throttleRequest);
//        ExecutorService executorService = Executors.newFixedThreadPool(numOfThreads);
//
//        throttler.addRule("bronze");
//        throttler.addRule("silver");
//        throttler.addRule("gold");
//
//        //flood the system
//        for (int i = 0; i < numTasks; i++) {
//            executorService.submit(task);
//        }
//
//        for (int j = 0; j < iterations; j++) {
//            long startTime = System.nanoTime();
//            throttler.isThrottled(throttleRequest);
//            resultList.add(System.nanoTime() - startTime);
//        }
//
//        executorService.shutdown();
//        Collections.sort(resultList);
//        double aggregateLatency = 0.0;
//        for (Long latency : resultList) {
//            aggregateLatency = +latency;
//        }
//        log.info("Avg Latency(ns) : " + aggregateLatency / iterations);
//        log.info("Max Latency(ns) : " + resultList.get(resultList.size() - 1));
//        log.info("Min Latency(ns) : " + resultList.get(0));
//    }
//
//    class ThrottlingTask implements Runnable {
//        Throttler throttler = null;
//        ThrottleRequest throttleRequest = null;
//
//        ThrottlingTask(Throttler throttler, ThrottleRequest throttleRequest) {
//            this.throttler = throttler;
//            this.throttleRequest = throttleRequest;
//        }
//
//        @Override
//        public void run() {
//                throttler.isThrottled(throttleRequest);
//        }
//    }
}
