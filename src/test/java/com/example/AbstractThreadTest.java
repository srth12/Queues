package com.example;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class AbstractThreadTest {

	public static void assertConcurrentTest(List<? extends Runnable> runners, int maxTimeout) throws InterruptedException {
		int noOfThreads = runners.size();
		CountDownLatch waitForAll = new CountDownLatch(noOfThreads);
		CountDownLatch awaitThreads = new CountDownLatch(1);
		CountDownLatch doneExecution = new CountDownLatch(noOfThreads);
		List<Exception> exceptionsThrown = Collections.synchronizedList(new ArrayList<>());
		ExecutorService executors = Executors.newFixedThreadPool(noOfThreads);
		
		for(Runnable runner : runners) {
			executors.submit(() -> {
				waitForAll.countDown();
				try {
					awaitThreads.await();
					runner.run();
				} catch (InterruptedException e) {
					exceptionsThrown.add(e);
				} finally {
					doneExecution.countDown();
				}
				
			});
		}
		awaitThreads.countDown();
		assertTrue("Exception Thrown on executing threads", exceptionsThrown.isEmpty());
		assertTrue("Timeout! Took more than " + maxTimeout + " seconds", doneExecution.await(maxTimeout, TimeUnit.SECONDS));
	}
}
