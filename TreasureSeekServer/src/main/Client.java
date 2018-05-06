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
	
	public static void main(String[] args) throws UnknownHostException, IOException, InterruptedException {
		System.out.println("On Client:");
		Client cl = new Client();
	}
	
	public Client() throws UnknownHostException, IOException, InterruptedException{
	  
	  String ask = "RETRIEVE_HOST\n";
	  String infoFromLoadBalancer;

	  Socket clientSocket = new Socket("localhost", serverPort);
	  
	  DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
	  //PrintWriter pw = new PrintWriter(outToServer);
	  BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
	  
	  //pw.println(ask);
	  
	  outToServer.write(ask.getBytes());
	  System.out.println("Sent message to lb " + ask);
	  Thread.sleep(2000);
	  infoFromLoadBalancer = inFromServer.readLine();
	  System.out.println("Received from Server: " + infoFromLoadBalancer);
	  //pw.close();
	  clientSocket.close();

	}

}
