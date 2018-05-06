package main;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

import org.json.JSONException;

import communications.Message;
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
		System.out.println("---Load Balancer---");
		Utils.setSecurityProperties();
		new LoadBalancer();
	}

	private class ConnectionHandler implements Runnable {

		private Socket socket;
		private Scanner socketIn;
		private PrintWriter socketOut;

		public ConnectionHandler(Socket socket) throws IOException {
			this.socket = socket;
			this.socketIn = new Scanner(new InputStreamReader(socket.getInputStream()));
			this.socketOut = new PrintWriter(new DataOutputStream(socket.getOutputStream()));
		}

		@Override
		public void run() {
			try {
				Message message = null;
				
				if(socketIn.hasNextLine()) {
					message = Message.parseMessage(socketIn.nextLine());
				}
				
				socketIn.close();
										
				if(message != null)
					handleMessage(message);
				
				socket.close();				
				
			} catch (IOException | ParseMessageException | JSONException e) {
				e.printStackTrace();
			}
			
		}
		
		public void handleMessage(Message message) throws IOException {
			
			Message.MessageType msgType = message.getHeader().getMessageType();
			
			switch (msgType) {

			case RETRIEVE_HOST:
				Pair<String, String> serverInfo = selectServer();
				this.socketOut.println(serverInfo.key + " " + serverInfo.value + '\n');
				break;

			case NEW_SERVER:
				// TODO add server IP and port to availableServers
				System.out.println("NEW_SEVER MESSAGE\n" + message.toString());
				break;

			default:
				break;

			}
		}
	}

	public LoadBalancer() throws IOException, ParseMessageException, JSONException {

		threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
		availableServers = new ArrayList<Pair<String, String>>();
		
		clientSocket = new ServerSocket(CLIENT_PORT);
		
		// hard-coded for now, will come from app server
		availableServers.add(new Pair<String, String>("IP1", "60"));
		availableServers.add(new Pair<String, String>("IP2", "61"));
		availableServers.add(new Pair<String, String>("IP3", "62"));

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

		Pair<String, String> server = availableServers.get(0);

		availableServers.remove(0);
		availableServers.add(server);

		return server;
	}

}