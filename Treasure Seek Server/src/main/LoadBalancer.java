package main;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

import org.json.JSONException;

import communications.Message;
import util.ParseMessageException;
import util.Utils.Pair;

public class LoadBalancer {

	static int serverPort = 6789;
	
	private ArrayList<Pair<String, String>> availableServers = new ArrayList<Pair<String, String>>();
	private ServerSocket welcomeSocket;
	
	public static void main(String[] args) throws IOException, ParseMessageException, JSONException {
		System.out.println("On Load Balancer");
		LoadBalancer lb = new LoadBalancer();
	}

	public LoadBalancer() throws IOException, ParseMessageException, JSONException {

		this.welcomeSocket = new ServerSocket(serverPort);

		availableServers.add(new Pair<String, String>("IP1", "60"));
		availableServers.add(new Pair<String, String>("IP2", "61"));
		availableServers.add(new Pair<String, String>("IP3", "62"));

		this.dispatcher();
		

	}

	public Pair<String, String> selectServer() {

		Pair<String, String> server = availableServers.get(0);

		availableServers.remove(0);
		availableServers.add(server);

		return server;
	}

	public void dispatcher() throws IOException, ParseMessageException, JSONException {

		String incomingMsg;
	
		while (true) {

			Socket connectionSocket = this.welcomeSocket.accept();
			
			Scanner in = new Scanner(new InputStreamReader(connectionSocket.getInputStream()));
			incomingMsg = in.nextLine();
			
			DataOutputStream out = new DataOutputStream(connectionSocket.getOutputStream());

			System.out.println("Received message: " + incomingMsg);
			Message message = Message.parseMessage(incomingMsg.getBytes());
			
			this.handleMessage(message, out);
			
		}
		
	}
	
	
	public void handleMessage(Message message, DataOutputStream out) throws IOException {
		
		Message.MessageType msgType = message.getHeader().getMessageType();
		
		switch(msgType) {
		
		case RETRIEVE_HOST:
			Pair<String, String> serverInfo = selectServer();
			out.writeBytes(serverInfo.key + " " + serverInfo.value + '\n');
			break;
			
		//TODO: NEW SERVER
			
		default:
			break;
		
		}
	}

}