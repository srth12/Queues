package com.example;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.amazonaws.services.sqs.model.Message;

public class InMemoryQueueService implements QueueService {

	private ConcurrentMap<String, InMemoryQueue> queues;
	
	public InMemoryQueueService() {
		queues = new ConcurrentHashMap<>();
	}
	
	/**
	 * Calls this function to add the new queue. 
	 * @param queueName Name of the queue to be added
	 * @return Returns True if the queue is successfully added or else false( if queue already exists)
	 */
	public boolean addNewQueue(String queueName) {
		if(queues.containsKey(queueName)) {
			return false;// if key already exist return false
		}
		return queues.put(queueName, new InMemoryQueue()) == null;
	}
	
	/**
	 * This method will pushes the message to the queue
	 * @param message The message to be added into the queue
	 * @param queueIdentifier Name of the queue to which message required to add
	 */
	@Override
	public void push(String message, String queueIdentifier) {
		queues.get(queueIdentifier).add(message);
	}

	/**
	 * This method will get the topmost message from the specified queue
	 * @param queueIdentifier The name of the queue from which message need to be pulled
	 */
	@Override
	public Message pull(String queueIdentifier) {
		return queues.get(queueIdentifier).poll();
	}

	/**
	 * Deletes the matching message from the specified queue
	 * @param receiptHandle Id of the message to be deleted, which was recieved of pulling the message
	 * @param queueIdentifier The name fo teh queue from which message need to be deleted
	 */
	@Override
	public void delete(String receiptHandle, String queueIdentifier) {
		queues.get(queueIdentifier).delete(receiptHandle);
	}

}
