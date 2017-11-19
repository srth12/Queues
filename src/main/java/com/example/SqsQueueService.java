package com.example;

import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;

public class SqsQueueService implements QueueService {
	//
	// Task 4: Optionally implement parts of me.
	//
	// This file is a placeholder for an AWS-backed implementation of QueueService.
	// It is included
	// primarily so you can quickly assess your choices for method signatures in
	// QueueService in
	// terms of how well they map to the implementation intended for a production
	// environment.
	//

	private AmazonSQSClient sqsClient;

	public SqsQueueService(AmazonSQSClient sqsClient) {
		this.sqsClient = sqsClient;
	}

	@Override
	public void push(String message, String queueIdentifier) {
		this.sqsClient.sendMessage(queueIdentifier, message);
	}

	@Override
	public Message pull(String queueIdentifier) {
		ReceiveMessageRequest request = new ReceiveMessageRequest().withQueueUrl(queueIdentifier)
				.withMaxNumberOfMessages(1);
		ReceiveMessageResult result = this.sqsClient.receiveMessage(request);
		return result.getMessages().get(0);
	}

	@Override
	public void delete(String receiptHandle, String queueIdentifier) {
		this.sqsClient.deleteMessage(queueIdentifier, receiptHandle);
	}
}
