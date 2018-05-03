package main;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import communications.Message;

public class Client {
	
	static int serverPort = 6789;
	
	public static void main(String[] args) throws UnknownHostException, IOException {
		System.out.println("On Client:");
		Client cl = new Client();
	}
	
	public Client() throws UnknownHostException, IOException{
	  
	  String ask = "RETRIEVE_HOST";
	  String infoFromLoadBalancer;

	  Socket clientSocket = new Socket("localhost", serverPort);
	  DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
	  PrintWriter pw = new PrintWriter(outToServer);
	  BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
	  
	  pw.println(ask);
	  infoFromLoadBalancer = inFromServer.readLine();
	  System.out.println("Received from Server: " + infoFromLoadBalancer);
	  clientSocket.close();

	}

}
