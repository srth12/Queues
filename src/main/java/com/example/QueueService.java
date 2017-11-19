package com.example;

import com.amazonaws.services.sqs.model.Message;

public interface QueueService {

  //
  // Task 1: Define me.
  //
  // This interface should include the following methods.  You should choose appropriate
  // signatures for these methods that prioritise simplicity of implementation for the range of
  // intended implementations (in-memory, file, and SQS).  You may include additional methods if
  // you choose.
  //
  // - push
  //   pushes a message onto a queue.
	
	/**
	 * pushes a message onto a queue.
	 * @param message
	 */
	public void push(String message, String queueIdentifier);
	
  // - pull
  //   retrieves a single message from a queue.
	public Message pull(String queueIdentifier);
  // - delete
  //   deletes a message from the queue that was received by pull().

	public void delete(String receiptHandle, String queueIdentifier);
}
