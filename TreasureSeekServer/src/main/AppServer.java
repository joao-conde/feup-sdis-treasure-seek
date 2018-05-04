package main;

import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.sql.SQLException;
import javax.rmi.ssl.SslRMIClientSocketFactory;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;

import org.json.JSONException;

import communications.Message;
import communications.Message.MessageType;
import controller.UserController;
import util.ParseMessageException;

public class AppServer{

	public static final int SERVER_CLIENT_PORT = 2000;
	public static String[] ENC_PROTOCOLS = new String[] {"TLSv1.2"};
	public static String[] ENC_CYPHER_SUITES = new String[] {"TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256"};
		
    private static final int REGISTRY_PORT = 1099;

	private static String[] dbServerIPs;
	private ExecutorService threadPool = Executors.newFixedThreadPool(20);
    
  
    public static void main(String[] args) throws RemoteException, NotBoundException, SQLException, UnknownHostException {
		
		setSecurityProperties();
    	
//		dbServerIPs = args;
//	
//		Registry registry = LocateRegistry.getRegistry(
//                InetAddress.getLocalHost().getHostName(), REGISTRY_PORT,
//                new SslRMIClientSocketFactory());
//        DBOperations dbOperations = (DBOperations) registry.lookup("db_1");

//         dbOperations.insertUser("leonardomgt", "leo@exemplo.com", "qwertyuioplkjhgfdsa");
 
        String loadBalancerHost = args[0];
        
        new AppServer(loadBalancerHost);
            
    }
    
	
    public AppServer(String loadBalancerHost) {
		
    		try {
			announceToLB(loadBalancerHost);
			receiveCalls();
		} catch (IOException e) {
			
			System.out.println("Could not announce me to load balancer");
		}
		
    	
    }
	
	
	private void receiveCalls() {
    	
		SSLServerSocket serverSocket = null;
		SSLServerSocketFactory factory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
		
		try {
			
			serverSocket = (SSLServerSocket) factory.createServerSocket(SERVER_CLIENT_PORT);
			serverSocket.setNeedClientAuth(false);
			serverSocket.setEnabledProtocols(ENC_PROTOCOLS);			
			serverSocket.setEnabledCipherSuites(serverSocket.getSupportedCipherSuites());
			
			
			while(true) {
				
				try {
					
					SSLSocket socket = (SSLSocket) serverSocket.accept();
					
					System.out.println("Message Received: ");
					
					HandleClientRequest handler = new HandleClientRequest(socket);
					threadPool.execute(handler);
					
					
				} catch (IOException e) {
					e.printStackTrace();
				}
				
			}
		}
		
		catch(IOException e) {
			
			e.printStackTrace();
			System.out.println(e.getMessage());
			
		}
		
		
    	
    	
    }
    
	private static void setSecurityProperties() {
		 
		String password = "123456";
		System.setProperty("javax.net.ssl.keyStore", "../security/keys/keystore");
		System.setProperty("javax.net.ssl.keyStorePassword", password);
		 		
		System.setProperty("javax.net.ssl.trustStore", "../security/certificates/truststore");
		System.setProperty("javax.net.ssl.trustStorePassword", password);
		
	}
	
	
    class HandleClientRequest implements Runnable {
		
		SSLSocket socket;
		
		public HandleClientRequest(SSLSocket socket) {
			this.socket = socket;
		}
		
		@Override
		public void run() {
			
			try {
				//				PrintWriter pw = new PrintWriter(this.socket.getOutputStream());
				BufferedReader br = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
				Scanner scanner = new Scanner(br);
				
				if(scanner.hasNextLine()) {
					
					String messageString = scanner.nextLine();
					System.out.println(messageString);
					
					Message messageReceived = Message.parseMessage(messageString);
					
					UserController.loginUser(messageReceived.getBody().getString("token"));
					System.out.println(messageReceived);
					
				}
				
			
				scanner.close();
				
				return;
				
				
			} catch (IOException | ParseMessageException | JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.out.println(e.getMessage());
			}
			
			
		}
    	
    }
    
    
    public void announceToLB(String loadBalancerHost) throws UnknownHostException, IOException {
    	
    		Socket socket = new Socket(InetAddress.getByName(loadBalancerHost), 7000);

    		PrintWriter pw = new PrintWriter(socket.getOutputStream());
    		Scanner scanner = new Scanner(new BufferedReader(new InputStreamReader(socket.getInputStream())));
    		
    		pw.println("Ola");
    		pw.flush();
    		pw.close();
    		
    		
    		if(scanner.hasNextLine()) {
        		String response = scanner.nextLine();
        		System.out.println(response);
    		}
    	
    		
    		
    		
    		scanner.close();
    		socket.close();
    		
    	
    }
    
    
    
	
}