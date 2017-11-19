package com.example;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Test;

import com.amazonaws.services.sqs.model.Message;

public class FileQueueServiceTest {

	/**
	 * A simple test case on top of regress test cases defined in the FileQueue, which do all the basic functionalities once
	 */
	@Test
	public void test() {
		
		try {
			Files.deleteIfExists(Paths.get("./serviceTest/lock"));
			Files.deleteIfExists(Paths.get("./serviceTestVisibility"));
			Files.deleteIfExists(Paths.get("./serviceTest"));
			if(Files.isDirectory(Paths.get("./queues"))) {
				FileQueue.deleteFolders("./queues");
			}

			FileQueue.deleteFolders("./serviceTestVisibilityLock");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		FileQueueService qService = new FileQueueService();
		String qName = "serviceTest";
		qService.push("Trial Message", qName);
		Message msg = qService.pull(qName);
		assertEquals("Trial Message", msg.getBody());
		qService.delete(msg.getReceiptHandle(), qName);
		assertEquals(null, qService.pull(qName));
	}

}
