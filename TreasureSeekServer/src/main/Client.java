package main;


import java.io.IOException;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.json.JSONException;
import org.json.JSONObject;

import communications.Message;
import communications.ReplyMessage;
import model.Model;
import util.ParseMessageException;
import util.Utils;

public class Client {
	
	private static final String TOKEN = "EAAYkUsBh9GgBAC2TZBeUNEPAYXMZCSJpyDUSJuK8KS6ZCp7yAd5xDBdRKpXJ2lfL3vOzFAWKm26ZBbyNQyTcgYLkOgg7Cj7j6rSGQzJ3DLhKiU5sVZBoUPWfmTee7C4Sr5ACC1p72q1jgBcErSLIqtpFRtLagfdAYURvAodnXhrZCz2cJTgttLFCMpXYBz4hvMhzPCw9mjOBfxL3FLs5EvzEDtj9adVS4ZD";
	
	private static final int LOAD_BALANCER_PORT = 6789;
	
    private static final String[] ENC_PROTOCOLS = new String[] {"TLSv1.2"};
	private static final int TIME_OUT = 3000;

	private String loadBalancerHost;
	private JSONObject requestJSON = new JSONObject();
	
	private int timesCompleted = 0;
	
	private ExecutorService threadPool = Executors.newFixedThreadPool(100);

	public static void main(String[] args) throws UnknownHostException, IOException, InterruptedException, NumberFormatException, ParseMessageException, JSONException {
		
		Utils.setSecurityProperties(false); 
		
		new Client(args[0], Integer.parseInt(args[1]),Integer.parseInt(args[2]), Integer.parseInt(args[3]));
		
	
		System.out.println("---CLIENT FINISHED---");
		
		
	}

	public Client(String loadBalancerHost,  int times, int timeOut, int action)
			throws UnknownHostException, IOException, InterruptedException, JSONException, ParseMessageException {

		this.loadBalancerHost = loadBalancerHost;
		
		requestJSON.put("token", TOKEN);
		requestJSON.put("address", "");
		
		if(action == 0) {
			
			for(int i = 0; i < times; i++) {
				
				sendGets(i);
				
			}
			
		}
		
		else {
			
			for(int i = 0; i < times; i++) {
				
				sendInsert(i);
				
			}
			
		}
		
		
		
		threadPool.awaitTermination(timeOut, TimeUnit.SECONDS);
		System.out.println("Requests Attended:" + timesCompleted);
				
	}
	
	private void sendGets(int time) throws IOException, ParseMessageException, JSONException {
		
		class SendGet implements Runnable {

			@Override
			public void run() {
				
				Socket socket = new Socket();
		        
		        SocketAddress loadBalancerSocketAddres = new InetSocketAddress(loadBalancerHost, LOAD_BALANCER_PORT);
		        
		        try {
					socket.connect(loadBalancerSocketAddres, TIME_OUT);
			        ReplyMessage loadBalancerResponse = Utils.sendMessage(Message.MessageType.RETRIEVE_HOST.description, socket);
			        System.out.println("Time: " + time + loadBalancerResponse.toString());
			        
			        SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
			        SSLSocket sslSocket = (SSLSocket) factory.createSocket();
			        
			        sslSocket.setEnabledProtocols(ENC_PROTOCOLS);
			        sslSocket.setEnabledCipherSuites(sslSocket.getSupportedCipherSuites());

			        JSONObject response = loadBalancerResponse.getBody().getJSONObject(0);
			       
			        String appServerAdd = response.getString("host");
			        int appServerPort = response.getInt("port");
			        
			        SocketAddress appServerAddress = new InetSocketAddress(appServerAdd, appServerPort);
			        sslSocket.connect(appServerAddress,TIME_OUT);
			        
			        ReplyMessage appServerResponse = Utils.sendMessage(Message.MessageType.LOGIN.description + " " + Model.ModelType.USER.getModelName() + " " + requestJSON.toString(), sslSocket);
			        
			        if(appServerResponse != null)
			        		System.out.println("Time: " + time + appServerResponse.toString());

			        
			        timesCompleted++;


			        socket.close();
			       			        			        
			        
				} catch (IOException | ParseMessageException | JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		       	        
			}
			
		}
		
		
		
		threadPool.execute(new SendGet());
		
	}
	
	private void sendInsert(int time) throws IOException, ParseMessageException, JSONException {
		
		class SendGet implements Runnable {

			@Override
			public void run() {
				
				Socket socket = new Socket();
		        
		        SocketAddress loadBalancerSocketAddres = new InetSocketAddress(loadBalancerHost, LOAD_BALANCER_PORT);
		        
		        try {
					socket.connect(loadBalancerSocketAddres, TIME_OUT);
			        ReplyMessage loadBalancerResponse = Utils.sendMessage(Message.MessageType.RETRIEVE_HOST.description, socket);
			        System.out.println("Time: " + time + loadBalancerResponse.toString());
			        
			        SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
			        SSLSocket sslSocket = (SSLSocket) factory.createSocket();
			        
			        sslSocket.setEnabledProtocols(ENC_PROTOCOLS);
			        sslSocket.setEnabledCipherSuites(sslSocket.getSupportedCipherSuites());

			        JSONObject response = loadBalancerResponse.getBody().getJSONObject(0);
			        			        
			        requestJSON.put("latitude", 0.0);
			        requestJSON.put("longitude", 0.0);
			        requestJSON.put("userCreatorId", Long.parseLong("1883453695006163"));
			        requestJSON.put("description", "description" + time);
			        requestJSON.put("challenge", "challenge" + time);
			        requestJSON.put("answer", "answer" + time);
			        
			        String appServerAdd = response.getString("host");
			        int appServerPort = response.getInt("port");
			        
			        SocketAddress appServerAddress = new InetSocketAddress(appServerAdd, appServerPort);
			        sslSocket.connect(appServerAddress,TIME_OUT);
			        
			        ReplyMessage appServerResponse = Utils.sendMessage(Message.MessageType.CREATE.description + " " + Model.ModelType.TREASURE.getModelName() + " " + requestJSON.toString(), sslSocket);
			        
			        if(appServerResponse != null)
			        		System.out.println("Time: " + time + appServerResponse.toString());

			        
			        timesCompleted++;


			        socket.close();
			       			        			        
			        
				} catch (IOException | ParseMessageException | JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		       	        
			}
			
		}
		
		
		
		threadPool.execute(new SendGet());
		
	}
	
	
}
