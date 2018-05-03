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


public class LoadBalancer{
	
	static int serverPort = 6789;
	private ArrayList<Pair<String,String>> availableServers = new ArrayList<Pair<String,String>>();

    public static void main(String[] args) throws IOException, ParseMessageException, JSONException{
    	System.out.println("On Load Balancer");
        LoadBalancer lb = new LoadBalancer();
    }

    public LoadBalancer() throws IOException, ParseMessageException, JSONException{
    	
	 String clientSentence;
	 String capitalizedSentence;
	 availableServers.add(new Pair<String,String>("IP1","60"));
	 availableServers.add(new Pair<String,String>("IP2","61"));
	 availableServers.add(new Pair<String,String>("IP3","62"));

	 ServerSocket welcomeSocket = new ServerSocket(serverPort);
	
	  while (true) {
	   Socket connectionSocket = welcomeSocket.accept();
	   Scanner inFromClient =
	    new Scanner(new InputStreamReader(connectionSocket.getInputStream()));
	   DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());
	   clientSentence = inFromClient.nextLine();
	   System.out.println("Received from Client: " + clientSentence);
	   Message message = Message.parseMessage(clientSentence.getBytes());
	   
	   Pair<String, String> serverInfo = selectServer();
	   outToClient.writeBytes(serverInfo.key + " " + serverInfo.value + '\n');
	  }

    }
    
    public Pair<String,String> selectServer(){
    	
    	Pair<String, String> server = availableServers.get(0);
    	
    	availableServers.remove(0);
    	availableServers.add(server);
    	
		return server;	
    }
    
    public void dispatcher() {
    	
//    	executor = Executors.newFixedThreadPool(10);
//    	
//    	Runnable appServer = new AppServer();
//		executor.execute(appServer);
    	
    }
    
}