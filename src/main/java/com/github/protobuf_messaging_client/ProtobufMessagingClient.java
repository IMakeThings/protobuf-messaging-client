package com.github.protobuf_messaging_client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

import com.github.support.Logger;
import com.proto.Message;
import com.proto.messageServiceGrpc;
import com.proto.messageServiceGrpc.messageServiceBlockingStub;
import com.proto.messageServiceGrpc.messageServiceStub;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

public class ProtobufMessagingClient {
	private final ManagedChannel channel;
	private final messageServiceBlockingStub blockingStub;
	private final messageServiceStub asyncStub;
	BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
	
	
	public ProtobufMessagingClient(String host, int port) {
		channel = ManagedChannelBuilder.forAddress(host, port).build();
		blockingStub = messageServiceGrpc.newBlockingStub(channel);
		asyncStub = messageServiceGrpc.newStub(channel);
	}
	
	public void shutdown() throws InterruptedException {
	    channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
	}
	
	public void receiveMessages() {
		try {
			Logger.log("Enter a username");
			String userName = br.readLine();
			
			final Message message = Message.newBuilder()
					.setMessage("")
					.setSender(userName)
					.build();
			
			blockingStub.receiveMessages(message);
			
			StreamObserver<Message> responseObserver = new StreamObserver<Message>() {
				@Override
				public void onNext(Message value) {
					Logger.log(value.getSender() + ":\t" + value.getMessage());
				}

				@Override
				public void onError(Throwable t) {
					Logger.log(t.getMessage());
				}

				@Override
				public void onCompleted() {
					// TODO Auto-generated method stub
					
				}
			};
			
			asyncStub.receiveMessages(message, responseObserver);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
}
