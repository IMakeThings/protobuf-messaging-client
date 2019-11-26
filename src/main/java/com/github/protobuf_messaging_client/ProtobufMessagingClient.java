package com.github.protobuf_messaging_client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import com.github.support.Logger;
import com.proto.Message;
import com.proto.messageServiceGrpc;
import com.proto.messageServiceGrpc.messageServiceBlockingStub;
import com.proto.messageServiceGrpc.messageServiceStub;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

public class ProtobufMessagingClient {
	private final ManagedChannel channel;
	private final messageServiceBlockingStub blockingStub;
	private final messageServiceStub asyncStub;
	private static final BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
	
	String userName;
	
	Random rand;
	
	public ProtobufMessagingClient(String host, int port) {
		channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
		blockingStub = messageServiceGrpc.newBlockingStub(channel);
		asyncStub = messageServiceGrpc.newStub(channel);
		
		rand = new Random();
	}
	
	public void init() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
	      @Override
	      public void run() {
	        // Use stderr here since the logger may have been reset by its JVM shutdown hook.
	        System.err.println("*** Disconnecting from server");
	        disconnect();
	        System.err.println("*** Disconnected");
	      }
	    });
		
		Logger.log("Enter a username");
		try {
			userName = br.readLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if(userName == null) {
			userName = Integer.toString(rand.nextInt());
		}
		
		Logger.log("Username: " + userName);
	}
	
	public void disconnect() {
		blockingStub.disconnect(Message.getDefaultInstance());
		try {
			shutdown();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void shutdown() throws InterruptedException {
	    channel.shutdown();
	}
	
	public void sendMessage(Message message) {
		try {
			blockingStub.sendMessage(message);
		} catch (StatusRuntimeException e) {
			Logger.log("Unable to send message: " + message.getMessage());
			return;
		}
	}
	
	public void receiveMessages() {
		StreamObserver<com.proto.Message> responseObserver =
				new StreamObserver<com.proto.Message>() {

					@Override
					public void onCompleted() {
						// TODO Auto-generated method stub
						disconnect();
					}

					@Override
					public void onError(Throwable arg0) {
						// TODO Auto-generated method stub
						
					}

					@Override
					public void onNext(Message arg0) {
						// TODO Auto-generated method stub
						Logger.log(arg0.getSender() + ":\t" + arg0.getMessage());
					}
			
		};
		
		Message connectMessage = Message.newBuilder()
				.setMessage("")
				.setSender(userName)
				.build();
		
		Iterator<Message> messages;
		
		asyncStub.receiveMessages(connectMessage, responseObserver);
	}
	
	public static void main(String[] args) {
		try {
			Logger.log("Enter a host name");
			String hostName = br.readLine();
			Logger.log("Enter a port");
			int port = Integer.parseInt(br.readLine());
			
			ProtobufMessagingClient pmc = new ProtobufMessagingClient(hostName, port);
			pmc.init();
			pmc.receiveMessages();
			
			while(true) {
				System.out.println("> ");
				String m = br.readLine();
				
				Message message = Message.newBuilder()
						.setSender(pmc.userName)
						.setMessage(m)
						.build();
				
				pmc.sendMessage(message);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
