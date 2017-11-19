package com.example;

import java.io.IOException;

import com.amazonaws.services.sqs.model.Message;

public class FileQueueService implements QueueService {
	//
	  // Task 3: Implement me if you have time.
	  //
	private FileQueue queue;
	
	public FileQueueService() {
		queue = new FileQueue();
	}
	
	/**
	 * Function will help to push the message into the File Queue
	 * @param message the message to be added into the queue
	 * @param queueIdentifier The name of the queue to which the message adds
	 */
	@Override
	public void push(String message, String queueIdentifier) {
		try {
			queue.addMessage(queueIdentifier, message);
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * This function will get the top most added message
	 * @param queueIdentifier The name of the queue from which message required to read
	 */
	@Override
	public Message pull(String queueIdentifier) {
		Message message = null;
		try {
			message = queue.readMessage(queueIdentifier);
			if(message == null || message == null)
				return null;
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
		return message;
	}

	/**
	 * This function will delete the message that have been pulled previously
	 * @param receiptHandle  The id that has received on pulling the message from queue
	 * @param queueIdentifier The name of the queue from the message required to delete
	 */
	@Override
	public void delete(String receiptHandle, String queueIdentifier) {
		try {
			queue.delete(queueIdentifier, receiptHandle);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
  
}
