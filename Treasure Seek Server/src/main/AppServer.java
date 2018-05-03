package main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.sql.SQLException;
import java.util.Arrays;
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
import util.ParseMessageException;

public class AppServer{

	public static final int SERVER_CLIENT_PORT = 2000;
	public static String[] ENC_PROTOCOLS = new String[] {"TLSv1.2"};
	public static String[] ENC_CYPHER_SUITES = new String[] {"TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256"};
		
    private static String[] dbServerIPs;
    private ExecutorService threadPool = Executors.newFixedThreadPool(200);
    
  
    public static void main(String[] args) throws RemoteException, NotBoundException, SQLException {
    	
	    	System.setProperty("javax.net.ssl.keyStore", "../security/keys/keystore");
		System.setProperty("javax.net.ssl.keyStorePassword", "123456");
	    	
//	    	int registryPort = 1099;
//	    	
//	    	
//	    	if (args.length > 0)
//	    		registryPort = Integer.parseInt(args[0]);
//	    	
//		if(args.length > 1)
//			dbServerIPs = Arrays.copyOfRange(args, 1, args.length);
//	
//	        
//        Registry registry = LocateRegistry.getRegistry(registryPort);
//        DBOperations dbOperations = (DBOperations) registry.lookup("db_operations");
//        
//        dbOperations.insertUser("leonardomgt", "leo@exemplo.com", "qwertyuioplkjhgfdsa");
        
        new AppServer();
            
    }
    
    
 
    public AppServer() {
	
    		receiveCalls();
    	
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
				
				String messageString = scanner.nextLine();
				System.out.println(messageString);

				
				Message messageReceived = Message.parseMessage(scanner.nextLine());
				
				System.out.println(messageReceived.getHeader().getMessageType());
				
				
				scanner.close();
				
				
			} catch (IOException | ParseMessageException | JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.out.println(e.getMessage());
			}
			
			
		}
    	
    }
    
    
    
    
    
   
}