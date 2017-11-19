package com.example;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import com.amazonaws.services.sqs.model.Message;

public class FileQueue {

	/**
	 * Visibility timeout of the queue in seconds
	 */
	private long visibilityTimeout=1000l;

	private ScheduledExecutorService executorService;

	public FileQueue() {
		executorService = Executors.newSingleThreadScheduledExecutor();
	}

	/**
	 * This function will create a queue if not exist and return true or else false
	 * 
	 * @param queueName
	 *            - name of the queue to be passed
	 * @return
	 * @throws IOException
	 */
	public boolean createQueue(String queueName) throws IOException {
		File file = new File("./queues/" + queueName);
		return file.createNewFile();
	}
	
	/**
	 * Check the queue if expired the visibility. If so, move back to the top of the queue.
	 * This method helps to make the queue cansistant even if the jvm crashes 
	 * @param queueName - the queue to be checked. Best if used before trying to read the
	 * message from queue
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	public void checkAndMoveTimedoutMessages(String queueName) throws IOException, InterruptedException {
		String fileName = "./queues/"+queueName+"Visibility";
		File f = new File(fileName);
		File lock = this.getLockFile(queueName+"VisibilityLock");
        this.lock(lock);
		if(!f.exists()) {
			this.unlock(lock);
			return;
		}
		Path visibilityFilePath = Paths.get("./queues/"+queueName+"Visibility");
		List<String> stringsToAdd = new ArrayList<>();
		System.out.println(f.getPath());
		String result = Files.lines(visibilityFilePath).map(x ->{
			Scanner in = new Scanner(x);
			if(in.hasNext()) {
				in.next();
				long timeInMilli = in.nextLong();
				System.out.println(timeInMilli);
				if(timeInMilli <= System.currentTimeMillis() && in.hasNext()) {
					String text ="";
					while(in.hasNext()) {
						text+=" "+in.next();
					}
					stringsToAdd.add(text.trim()+"\n");
					in.close();
					return null;
				}
			}
			in.close();
			return x;
		}).filter(x->(x != null) && (x.trim()!=null) && !x.trim().equals("")).reduce("", (a, b) -> { return a + "\n" + b;});
		if(!result.trim().equals("")) {
			Files.write(visibilityFilePath, (result).getBytes("utf-8"), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
		}else {
			if(visibilityFilePath.toFile().exists()) {
				Files.delete(visibilityFilePath);
			}
		}
		this.unlock(lock);
		for(String msg: stringsToAdd) {
			addFirst(queueName, msg);
		}
		
	}
	/**
	 * This method need to be called to get the locking file, so that we can call the lock.
	 * @param queueName name of the queue to which we require the locking
	 * @return
	 */
	private File getLockFile(String queueName) {
        return this.createFile(queueName, "lock");
    }

    private File createFile(String queueName, String filename) {
        File tempDir = this.createQueueDirectory(queueName);
        File file = new File(tempDir, filename);

        return file;
    }
    /**
     * Used to lock the queue
     * @param lock The value recieved from getLockFile(String queueName)
     * @throws InterruptedException
     */
    private void lock(File lock) throws InterruptedException {
        while (!lock.mkdir()) {
            Thread.sleep(50);
        }
    }
    
    private File createQueueDirectory(String queueName) {
        File tempDir = new File("./", queueName);

        if(!tempDir.exists()) {
            tempDir.mkdir();
        }
        return tempDir;
    }
    
    private void unlock(File lock) {
        lock.delete();
    }
	
	/**
	 * This method will add the message to the queue specified. Handles multiple processors/ threads 
	 * @param queueName Name of the queue to which message required to be added
	 * @param message  - This is what will be adding into the queue
	 * @throws IOException  Should have caught the exceptions, but not doing due to lack of time
	 * @throws InterruptedException
	 */
	public void addMessage(String queueName, String message) throws IOException, InterruptedException {
		checkAndMoveTimedoutMessages(queueName);
		File lock = this.getLockFile(queueName);
        this.lock(lock);
		String fileName = "./queues/"+queueName;
		
		Path parent = Paths.get(fileName).getParent();
		if(parent != null)
			Files.createDirectories(parent);
		Files.write(Paths.get(fileName), (message+"\n").getBytes("utf-8"), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		this.unlock(lock);
	}
	
	/**
	 * This method can be called to clean the folders created, mostly useful for testing purpose
	 * @param rootPath  Path of the root folder to be deleted
	 * @throws IOException
	 */
	public static void deleteFolders(String rootPath) throws IOException {
		if(!Files.isDirectory(Paths.get("./queues"))) {
			return;
		}
		Files.walk(Paths.get(rootPath) , FileVisitOption.FOLLOW_LINKS)
	    .sorted(Comparator.reverseOrder())
	    .peek(System.out::println)
	    .forEach(t -> {
			try {
				Files.delete(t);
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}
	
	/**
	 * THis method will read the topmost message in the queue mentioned
	 * @param queueName  name of the queue to be read
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public Message readMessage(String queueName) throws IOException, InterruptedException {
		checkAndMoveTimedoutMessages(queueName);
		Path file = Paths.get("./queues/"+queueName+"Visibility");
		String uniqueRandomKey = InMemoryQueue.getRandomUniqueKey();
		String readMessage;
		File lock = this.getLockFile(queueName);
        this.lock(lock);
		if(!Files.exists(Paths.get("./queues/"+ queueName)))
			return null;
		Function<String, String> consumerForFirstLine = x ->{
			long timeout = System.currentTimeMillis() + this.visibilityTimeout;
			  try {
				Files.write(file, (uniqueRandomKey+" "+Long.toString(timeout)+" "+x+"\n").getBytes("utf-8"), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
			} catch (IOException e) {
				e.printStackTrace();
			} 
			  Runnable msgMovingTask = () -> {
//					logger.info("Adding message back to the queue"+message);
				try {
					System.out.println("***Adding message back to queue-"+x);
					addFirst(queueName, x);
					delete(queueName, uniqueRandomKey);
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				};
				scheduleMessageMovingTask(msgMovingTask);
				return x;
		};
		
		readMessage = Files.lines(Paths.get("./queues/"+ queueName)).filter(x->(x != null) && (x.trim()!=null) && !x.trim().equals("")).findFirst().
		map(consumerForFirstLine).get();
		Optional<String> remainingMessage = Files.lines(Paths.get("./queues/"+ queueName)).filter(x->(x != null) && (x.trim()!=null) && !x.trim().equals("")).skip(1)
		.reduce( (a, b) -> { return a + "\n" + b;});
		if(remainingMessage.isPresent()) {
			Files.write(Paths.get("./queues/"+ queueName), remainingMessage.get().getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
		}else {
			Files.deleteIfExists(Paths.get("./queues/"+ queueName));
		}
		
		this.unlock(lock);
		if(readMessage == null || readMessage.equals(""))
			return null;
		return new Message().withMessageId(uniqueRandomKey).withBody(readMessage).withReceiptHandle(uniqueRandomKey);
	}
	
	/**
	 * Addes the messages to the head of the queue, used mostly to add the expired undeleted messages that has read and was invisible to the top of the queue
	 * @param queueName
	 * @param message
	 * @throws IOException
	 */
	private void addFirst(String queueName, String message) throws IOException {
		String newList = "";
		if( Files.exists(Paths.get("./queues/"+ queueName))) {
			newList = Files.lines(Paths.get("./queues/"+ queueName)).filter(x->(x.trim()!=null) && !x.trim().equals("") && !x.trim().equals("\n")).reduce
					("", (a, b) -> { return a + "\n" + b;});
			newList = message+"\n"+newList+"\n";
		}else {
			newList = message+"\n";
		}
		
		Files.write(Paths.get("./queues/"+ queueName), (newList).getBytes("utf-8"), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
	}

	private ScheduledFuture<?> scheduleMessageMovingTask(Runnable task) {
		return executorService.schedule(task, visibilityTimeout, TimeUnit.SECONDS);
	}
	
	/**
	 * Returns the size of the queue passed
	 * @param queueName
	 * @return
	 * @throws IOException
	 */
	public long queueSize(String queueName) throws IOException {
		Path filePath = Paths.get("./queues/"+queueName);
		return Files.lines(filePath).filter(x->(x.trim()!=null) && !x.trim().equals("") && !x.trim().equals("\n")).count();
	}

	/**
	 * Deletes the messages that has polled from the specified queue
	 * @param queueName Name of the queue to be passed
	 * @param receiptHandle  the id of the message that must have been got when read
	 * @throws IOException
	 */
	public void delete(String queueName, String receiptHandle) throws IOException {
		Path visibilityFilePath = Paths.get("./queues/"+ queueName+"Visibility");
		if(!visibilityFilePath.toFile().exists() || !Files.exists(visibilityFilePath)) {
			return;
		}
		
		String newList = Files.lines(visibilityFilePath).filter
		(s -> {
			String[] strings = s.split(" ");
			if(strings[0].trim().equals(receiptHandle))
			 return false;
			else return true;
		}).filter(x->(x.trim()!=null) && !x.trim().equals("")).reduce("", (a, b) -> { return a + "\n" + b;});
		System.out.println(newList);
		if(!newList.trim().equals("")) {
		 Files.write(visibilityFilePath, (newList+"\n").getBytes("utf-8"), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
		}else {
			Files.deleteIfExists(visibilityFilePath);
		}
	}


}
