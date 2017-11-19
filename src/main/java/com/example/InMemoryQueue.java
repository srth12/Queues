package com.example;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.amazonaws.services.sqs.model.Message;


public class InMemoryQueue {

	private final static Logger logger = Logger.getLogger(InMemoryQueue.class.getName());
	
	private ConcurrentMap<String, ScheduledFuture<String>> invisibleMessages;
	private BlockingQueue<String> messages;
	private int visibilityTimeout = 120;
	private ScheduledExecutorService executorService;
	
	public InMemoryQueue() {
		executorService = Executors.newSingleThreadScheduledExecutor();
		messages = new LinkedBlockingDeque<>();
		invisibleMessages = new ConcurrentHashMap<>();
	}
	
	/**
	 * Pushes the message into the queue
	 * @param message - the element to add
	 */
	public void add(String message) {
		messages.add(message);
	}
	
	/**
	 * Returns the next element available in the queue wrapped in the 'Message' class. It also contains
	 * receiptHandle and message id which is required for deleting the message from queue later
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public Message poll() {
		String message = messages.poll();
		if(message == null)
			return null;
		logger.info("Message got from queue-" + message + ", with message size of "+ size());
		String uniqueRandomKey = getRandomUniqueKey();
		Runnable msgMovingTask = () -> {
			logger.info("Adding message back to the queue"+message);
			((LinkedBlockingDeque<String>) messages).addFirst(message);
			invisibleMessages.remove(uniqueRandomKey);
		};
		ScheduledFuture scheduledFuture = scheduleMessageMovingTask(msgMovingTask);
		invisibleMessages.put(uniqueRandomKey, scheduledFuture);
		return new Message().withMessageId(uniqueRandomKey).withBody(message).withReceiptHandle(uniqueRandomKey);
	}
	
	/**
	 * Returns the unique key
	 * @return the string representation of unique key
	 */
	public static String getRandomUniqueKey() {
		UUID uuid = UUID.randomUUID();
		return uuid.toString() + LocalDateTime.now();
	}

	/**
	 * The task will be scheduled to submit at the time specified in parameter- 'visibilityTimeout' 
	 * @param task
	 * @return 
	 */
	private ScheduledFuture<?> scheduleMessageMovingTask(Runnable task) {
		return executorService.schedule(task, visibilityTimeout, TimeUnit.SECONDS);
	}
	
	/**
	 * Removes the message from the queue on sending the receipt handler passed 
	 * @param receiptHandle - This value has been shared on requesting poll()
	 */
	public void delete(String receiptHandle) {
		ScheduledFuture<String> future = invisibleMessages.remove(receiptHandle);
		if(future == null) {
			throw new RuntimeException("Message doesn't exist");
		}
		future.cancel(true);
	}
	
	/**
	 * Returns the size of the queue
	 * @return return type will be 'int'
	 */
	public int size() {
		return messages.size();
	}
	
	/**
	 * Sets the timeout of invisibility of the message. The default is 120 seconds
	 * NB: This timeout will not affect the previously submitted results
	 * @param time - the input parameter in seconds( type- int)
	 */
	public void setVisibilityTimeout(int time) {
		this.visibilityTimeout = time;
	}
	
}
