package com.example;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class FileQueueTest {
  //
  // Implement me if you have time.
  //
	private FileQueue queue;
	
	@Before
	public void init() {
		queue = new FileQueue();
	}
 
	
	@Test
	public void shouldHandleCuncurrentAdd() throws InterruptedException, IOException {
		List<Runnable> runners = new ArrayList<>();
		Files.deleteIfExists(Paths.get("./queues/testAdd"));
		String queueName = "testAdd";
		for(int i=0;i< 100;i ++) {
			String msg = "Dummy Message -"+i;
			runners.add(()-> {
				try {
					queue.addMessage(queueName, msg);
				} catch (IOException | InterruptedException e) {
					e.printStackTrace();
				}
			});
		}
		
		AbstractThreadTest.assertConcurrentTest(runners, 1000);
		assertEquals(100, queue.queueSize(queueName));
	}
	
	@Test
	@Ignore
	public void shouldHandleConcurrentReadMessage() throws IOException, InterruptedException {
		List<String> addedMessages = Collections.synchronizedList(new ArrayList<>());
		List<Runnable> runners = new ArrayList<>();
		String queueName ="testRead";
		Files.deleteIfExists(Paths.get("./queues/testRead"));
		Files.deleteIfExists(Paths.get("./queues/testReadVisibility"));
		if(Files.isDirectory(Paths.get("./testReadVisibilityLock"))) {
			FileQueue.deleteFolders("./testReadVisibilityLock");
		}
		if(Files.isDirectory(Paths.get("./queues"))) {
			FileQueue.deleteFolders("./queues");
		}
		for(int i=0;i<100;i++) {
			String msg = "Test-"+i;
			
			queue.addMessage(queueName, msg);
			addedMessages.add(msg);
			runners.add(()->{
				try {
					String tempMsg = queue.readMessage(queueName).getBody();
					addedMessages.remove(tempMsg);
				} catch (IOException | InterruptedException e) {
					e.printStackTrace();
				}
			});
		}
		AbstractThreadTest.assertConcurrentTest(runners, 1000);
		assertEquals(0, addedMessages.size());
	}
	
	@Test
	public void shouldHandleCuncurrentDelete() throws IOException, InterruptedException {
		List<Runnable> runners = new ArrayList<>();
		String queueName ="testDelete";
		try {
			Files.deleteIfExists(Paths.get("./testDelete/lock"));
			Files.deleteIfExists(Paths.get("./testDeleteVisibility"));
			Files.deleteIfExists(Paths.get("./testDelete"));
			if(Files.isDirectory(Paths.get("./testDeleteVisibilityLock"))) {
				queue.deleteFolders("./testDeleteVisibilityLock");
			}
			if(Files.isDirectory(Paths.get("./queues"))) {
				queue.deleteFolders("./queues");
			}
		} catch (DirectoryNotEmptyException e2) {
			System.out.println("Not able to clear directory");
		}
		for(int i=0;i<100;i++) {
			String msg = "Test-"+i;
			queue.addMessage(queueName, msg);
			String msgReceiptHandle = queue.readMessage(queueName).getReceiptHandle();
			runners.add(()->{
				try {
					queue.delete(queueName, msgReceiptHandle);
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
		
		}
		AbstractThreadTest.assertConcurrentTest(runners, 1000);
		if(Files.exists(Paths.get("./queues/testReadVisibility"))) {
			Optional<String> result = Files.lines(Paths.get("./queues/testReadVisibility")).reduce((a,b)-> (a+b).trim());
			assertFalse(result.isPresent());
		}
	}
	
	@Test
	@Ignore
	public void testSizeOfQueue() throws IOException {
		String queueName = "testSize";
		Path filePath = Paths.get("./queues/testSize");
		String fileBody = "Sarath\nBabu";
		Files.write(filePath, fileBody.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
		assertEquals(2, queue.queueSize(queueName));
	}
}
