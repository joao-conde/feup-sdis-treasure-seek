package main;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.JSONException;

import communications.Message;
import util.ParseMessageException;
import util.Utils.Pair;

public class LoadBalancer {

	private final int THREAD_POOL_SIZE = 100;
	private final int SERVER_PORT = 6789;

	private ArrayList<Pair<String, String>> availableServers;
	private ServerSocket serverSocket;
	private ExecutorService threadPool;

	public static void main(String[] args) throws IOException, ParseMessageException, JSONException {
		System.out.println("On Load Balancer");
		new LoadBalancer();
	}

	private class ConnectionHandler implements Runnable {

		private Socket socket;
		private InputStreamReader socketIn;
		private DataOutputStream socketOut;

		public ConnectionHandler(Socket socket) throws IOException {
			this.socket = socket;
			this.socketIn = new InputStreamReader(socket.getInputStream());
			this.socketOut = new DataOutputStream(socket.getOutputStream());
		}

		@Override
		public void run() {
			try {
				Message message = readMessage();
				
				if(message != null)
					handleMessage(message);
				
				socket.close();
				
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}

		public Message readMessage() {

			try {
				Scanner in = new Scanner(this.socketIn);
				String receivedMsg = in.nextLine();
				//in.close(); if uncommented socket closes here, preventing
				//further message sending

				System.out.println("Received message: " + receivedMsg);

				return Message.parseMessage(receivedMsg);

			} catch (ParseMessageException | JSONException e) {
				e.printStackTrace();
			}
			
			return null;
		}
		
		public void handleMessage(Message message) throws IOException {
			
			Message.MessageType msgType = message.getHeader().getMessageType();
			
			switch (msgType) {

			case RETRIEVE_HOST:
				Pair<String, String> serverInfo = selectServer();
				this.socketOut.writeBytes(serverInfo.key + " " + serverInfo.value + '\n');
				break;

			case NEW_SERVER:
				// TODO add server IP and port to availableServers
				System.out.println("NEW_SEVER MESSAGE");
				break;

			default:
				break;

			}
		}
	}

	public LoadBalancer() throws IOException, ParseMessageException, JSONException {

		this.serverSocket = new ServerSocket(SERVER_PORT);
		this.threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
		this.availableServers = new ArrayList<Pair<String, String>>();

		// hard-coded for now, will come from app server
		availableServers.add(new Pair<String, String>("IP1", "60"));
		availableServers.add(new Pair<String, String>("IP2", "61"));
		availableServers.add(new Pair<String, String>("IP3", "62"));

		dispatcher();

	}

	public void dispatcher() throws IOException, ParseMessageException, JSONException {

		while (true) {
			Socket connectionSocket = this.serverSocket.accept();
			threadPool.execute(new ConnectionHandler(connectionSocket));
		}

	}

	public Pair<String, String> selectServer() {

		Pair<String, String> server = availableServers.get(0);

		availableServers.remove(0);
		availableServers.add(server);

		return server;
	}

}