package com.example;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.amazonaws.services.sqs.model.Message;


public class InMemoryQueueTest {

	private InMemoryQueue inMemoryQueue;
	
	@Before
	public void setup() {
		inMemoryQueue = new InMemoryQueue();
	}
	
	/**
	 * This test will check if the element polled is invisible before timeout and visible after timeout
	 * @throws InterruptedException
	 */
	@Test
	public void testPoll() throws InterruptedException {
		inMemoryQueue.setVisibilityTimeout(1);
		inMemoryQueue.add("abc");
		Message res = inMemoryQueue.poll();
		Assert.assertEquals(0, inMemoryQueue.size());
		Thread.sleep(1100);
		System.out.println(inMemoryQueue.size());
		Assert.assertEquals(1, inMemoryQueue.size());
		System.out.println(res);
	}
	
	/**
	 * This test case will check if the elements removed are added in front of the queue after visibility timeout
	 * @throws InterruptedException 
	 */
	@Test
	public void testPollForAddingMsgToHeaderOnTimeout() throws InterruptedException {
		inMemoryQueue.setVisibilityTimeout(1); 
		inMemoryQueue.add("abc");
		inMemoryQueue.add("def");
		inMemoryQueue.add("ghi");
		Message res = inMemoryQueue.poll();
		inMemoryQueue.poll();
		inMemoryQueue.poll();
		inMemoryQueue.delete(res.getReceiptHandle());
		System.out.println(inMemoryQueue.size());
		Thread.sleep(2000);
		res = inMemoryQueue.poll();
		Assert.assertEquals("ghi", res.getBody());
		res = inMemoryQueue.poll();
		inMemoryQueue.delete(res.getReceiptHandle());
		Assert.assertEquals("def", res.getBody());
	}
	
	@Test
	public void addTest() {
		inMemoryQueue.add("abc");
		Assert.assertEquals(inMemoryQueue.size(), 1);
		inMemoryQueue.add("abc");
		Assert.assertEquals(inMemoryQueue.size(), 2);
	}
	
	/**
	 * Tests the concurrently adding feature of Add()
	 * @throws InterruptedException
	 */
	@Test
	public void shouldHandleCuncurrentAdd() throws InterruptedException {
		List<Runnable> runners = new ArrayList<>();
		for(int i=0;i< 500;i ++) {
			runners.add(()-> {
				inMemoryQueue.add("Dummy Message");
			});
		}
		AbstractThreadTest.assertConcurrentTest(runners, 100);
		System.out.println(inMemoryQueue.size());
	}
	
	/**
	 * Tests the concurrently adding feature of poll()
	 * @throws InterruptedException
	 */
	@Test
	public void shouldHandleConcurrentPoll() throws InterruptedException {
		List<Runnable> runners = new ArrayList<>();
		List<String> msgAdded = Collections.synchronizedList(new ArrayList<>());
		for(int i=0;i< 500;i ++) {
				inMemoryQueue.add("Dummy Message"+i);
				msgAdded.add("Dummy Message"+i);
				runners.add(()->{
					assertTrue(msgAdded.remove(inMemoryQueue.poll().getBody()));
				}
				);
		}
		AbstractThreadTest.assertConcurrentTest(runners, 100);
	}
	/**
	 * Tests the concurrently adding feature of Delete()
	 * @throws InterruptedException
	 */
	@Test
	public void shouldHandleConcurrentDelete() throws InterruptedException {
		List<Runnable> runners = new ArrayList<>();
		for (int i = 0; i < 500; i++) {
			inMemoryQueue.add("Message "+i);
			Message msg = inMemoryQueue.poll();
			runners.add(()->inMemoryQueue.delete(msg.getReceiptHandle()));
		}
		AbstractThreadTest.assertConcurrentTest(runners, 100);
		assertTrue(inMemoryQueue.size()==0);
	}
	
	/**
	 * This simple test case checks if it won't create any constant
	 */
	@Test
	public void testRandomNumberGenerator() {
		Assert.assertNotEquals(InMemoryQueue.getRandomUniqueKey(),InMemoryQueue.getRandomUniqueKey());
	}
	
	
	
}
