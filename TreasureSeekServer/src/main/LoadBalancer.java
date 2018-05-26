package main;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import communications.Message;
import communications.ReplyMessage;
import communications.ReplyMessage.ReplyMessageStatus;
import util.ParseMessageException;
import util.Utils;
import util.Utils.Pair;



public class LoadBalancer {

	public static final int CLIENT_PORT = 6789;
	public static final int SERVER_PORT = 7000;
	public static String[] ENC_PROTOCOLS = new String[] {"TLSv1.2"};

	
	private final int THREAD_POOL_SIZE = 100;
	

	private ArrayList<Pair<String, String>> availableServers;
	private ServerSocket clientSocket;
	
	private ExecutorService threadPool;

	public static void main(String[] args) throws IOException, ParseMessageException, JSONException {
		if(Arrays.asList(args).indexOf("--help") != -1) {
			System.out.println(usage());
			System.exit(1);
		}
		
		System.out.println("---Load Balancer---");
		Utils.setSecurityProperties(false);
		new LoadBalancer();
	}
	

	private class ConnectionHandler implements Runnable {

		private Socket socket;
		
		public ConnectionHandler(Socket socket) throws IOException {
			this.socket = socket;
		}

		@Override
		public void run() {
			try {
				
				String reply = null;
				Message message = null;
				Scanner socketIn = new Scanner(new InputStreamReader(socket.getInputStream()));
				PrintWriter socketOut = new PrintWriter(socket.getOutputStream(), true);

				if(socketIn.hasNextLine()) {
					message = Message.parseMessage(socketIn.nextLine());
				}
														
				if(message != null)
					reply = handleMessage(message);
				
			
				socketOut.println(reply);				

				socketIn.close();
				socketOut.close();
				
			} catch (IOException | ParseMessageException | JSONException e) {
				e.printStackTrace();
			}
			
		}
		
		public String handleMessage(Message message) throws IOException, JSONException {
			
			String reply = null;
			JSONArray jsonArray = new JSONArray();
			JSONObject json = new JSONObject();
			
			Message.MessageType msgType = message.getHeader().getMessageType();
			
			System.out.println(message);

			switch (msgType) {

			case RETRIEVE_HOST:
				Pair<String, String> serverInfo = selectServer();
				
				if(serverInfo != null) {
					json.put("host", serverInfo.key);
					json.put("port", serverInfo.value);
					jsonArray.put(json);
					reply = ReplyMessage.buildResponseMessage(ReplyMessageStatus.OK, jsonArray);
				}
					
				else {
					json.put("message", "No available server");
					jsonArray.put(json);
					reply = ReplyMessage.buildResponseMessage(ReplyMessageStatus.BAD_REQUEST, jsonArray);
				}			
				
				break;

			case NEW_SERVER:
			
				Pair<String,String> newServerID = new Pair<String,String>(message.getBody().get("host").toString(), 
						message.getBody().get("port").toString());
				
				if(!availableServers.contains(newServerID)) {
					availableServers.add(newServerID);
					reply = ReplyMessage.buildResponseMessage(ReplyMessageStatus.OK);
				}
				else {
					json.put("message", "Server ID (<IP> <port>) already in use");
					jsonArray.put(json);
					reply = ReplyMessage.buildResponseMessage(ReplyMessageStatus.BAD_REQUEST, jsonArray);
				}
						
				break;
			
			case SHUTDOWN_SERVER:
				Pair<String,String> oldServerID = new Pair<String,String>(message.getBody().get("host").toString(), 
						message.getBody().get("port").toString());
				
				availableServers.remove(oldServerID);
				
				reply = ReplyMessage.buildResponseMessage(ReplyMessageStatus.OK);
				break;

			default:
				reply = ReplyMessage.buildResponseMessage(ReplyMessageStatus.BAD_REQUEST);
				break;

			}
			
			return reply;
		}
		
		
	}

	public LoadBalancer() throws IOException, ParseMessageException, JSONException {

		threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
		availableServers = new ArrayList<Pair<String, String>>();
		
		clientSocket = new ServerSocket(CLIENT_PORT);
		
		clientDispatcher();
		serverDispatcher();

	}

	public void clientDispatcher() throws IOException, ParseMessageException, JSONException {
		
		class ClientListener implements Runnable{
			
			@Override
			public void run() {
				while (true) {
					Socket connectionSocket;
					try {
						connectionSocket = clientSocket.accept();
						threadPool.execute(new ConnectionHandler(connectionSocket));
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		
		new Thread(new ClientListener()).start();

	}
	
	public void serverDispatcher() throws IOException, ParseMessageException, JSONException {

		class ServerListener implements Runnable {
			
			@Override
			public void run() {
				
				SSLServerSocket socket = null;
				SSLServerSocketFactory factory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault(); 
				
				try {
					
					socket = (SSLServerSocket) factory.createServerSocket(SERVER_PORT);
					socket.setNeedClientAuth(true);
					socket.setEnabledProtocols(ENC_PROTOCOLS);			
					socket.setEnabledCipherSuites(socket.getSupportedCipherSuites());
					
					while (true) {
						SSLSocket connectionSocket;
						try {
							connectionSocket = (SSLSocket) socket.accept();							
							threadPool.execute(new ConnectionHandler(connectionSocket));
							
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					
				}
				
				catch(IOException e) {
					
					e.printStackTrace();
					
				}
				
			}
		}
		
		new Thread(new ServerListener()).start();

	}

	public Pair<String, String> selectServer() {
	
		if(availableServers.size() == 0)
			return null;
		
		Pair<String, String> server = availableServers.get(0);

		availableServers.remove(0);
		availableServers.add(server);

		return server;
	}

	
	public static String usage() {
		
		ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
		PrintWriter out = new PrintWriter(outBuffer);
		
		out.println("Usage:");
		out.println("run_load_balancer.sh <args>:");
		out.println("\t<args>:");
		out.println("\t--help => Help");
		
		out.close();
		
		return outBuffer.toString();
		
	}

}