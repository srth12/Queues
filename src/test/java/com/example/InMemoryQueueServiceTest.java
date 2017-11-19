package com.example;

import static org.junit.Assert.*;

import org.junit.Test;

import com.amazonaws.services.sqs.model.Message;

public class InMemoryQueueServiceTest {

	/**
	 * A simple test case on top of regress test cases defined in the InMemoryQueue, which do all the basic functionalities once
	 */
	@Test
	public void test() {
		InMemoryQueueService qService = new InMemoryQueueService();
		String qName = "test";
		if(qService.addNewQueue(qName)) {
			qService.push("Trial Message", qName);
			Message msg = qService.pull(qName);
			assertEquals("Trial Message", msg.getBody());
			qService.delete(msg.getReceiptHandle(), qName);
			assertEquals(null, qService.pull(qName));
		}
	}

}
