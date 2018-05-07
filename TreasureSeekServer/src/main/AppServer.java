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

import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;

import org.json.JSONException;
import org.json.JSONObject;

import communications.Message;
import controller.UserController;
import util.ParseMessageException;
import util.Utils;

public class AppServer{

	public static String[] ENC_PROTOCOLS = new String[] {"TLSv1.2"};
	public static String[] ENC_CYPHER_SUITES = new String[] {"TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256"};
		
    private static final int REGISTRY_PORT = 1099;

	private static String[] dbServerIPs;
	
	private ExecutorService threadPool = Executors.newFixedThreadPool(20);
	public int serverClientPort;
	
	private UserController userController;
	private DBOperations dbOperations;

  
    public static void main(String[] args) throws InterruptedException {
		
        String loadBalancerHost = args[0];
        int clientServerPort = Integer.parseInt(args[1]);
        
        new AppServer(loadBalancerHost, clientServerPort);
            
    }
    
    
    private DBOperations getDBOperations() throws RemoteException, NotBoundException, SQLException, UnknownHostException {
    	
    		Registry registry = LocateRegistry.getRegistry(
    				InetAddress.getLocalHost().getHostName(), REGISTRY_PORT,
                new SslRMIClientSocketFactory());
         
    		dbOperations = (DBOperations) registry.lookup("db_1");
    		return dbOperations;

    }
    
	
    public AppServer(String loadBalancerHost, int serverClientPort) throws InterruptedException {
		
    	Utils.setSecurityProperties();
    	
		//dbServerIPs = args;
	
    		this.serverClientPort = serverClientPort;
    	
    		try {
			announceToLB(loadBalancerHost);
			this.dbOperations = getDBOperations();    	
//    		this.userController = new UserController(dbOperations);
//			receiveCalls();
		
    		} catch (SSLHandshakeException e) {
    			
    			System.out.println("App Server could not handshake with load balancer/Db Server on host " + loadBalancerHost);
			
		} catch(UnknownHostException e) {
			
			System.out.println("App Server could not connect to load balancer or Db Server");
			
		}
    		
    		catch ( IOException e) {
			
    			System.out.println("App Server could not connect to Load Balancer or Db Server");
		
    		} 
    		
    		catch (NotBoundException e) {
			
    			System.out.println("App Server could not connect to remote rmi object" + loadBalancerHost);
		
    		} catch (SQLException e) {
			System.out.println(e.getLocalizedMessage());
		}
    		
    		
    		  	
    		this.userController = new UserController(dbOperations);
    		receiveCalls();
		
    	
    }
	
	
	private void receiveCalls() {
    	
		SSLServerSocket serverSocket = null;
		SSLServerSocketFactory factory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
		
		try {
			
			serverSocket = (SSLServerSocket) factory.createServerSocket(serverClientPort);
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
				
				if(scanner.hasNextLine()) {
					
					String messageString = scanner.nextLine();
					System.out.println(messageString);
					
					Message messageReceived = Message.parseMessage(messageString);
					
					userController.loginUser(messageReceived.getBody().getString("token"));
					
				
					System.out.println(messageReceived);
					
				}
				
			
				scanner.close();
				
				return;
				
				
			} catch (IOException | ParseMessageException | JSONException e) {
				e.printStackTrace();
				System.out.println(e.getMessage());
			}
			
			
		}
    	
    }
    
    
    public void announceToLB(String loadBalancerHost) throws UnknownHostException, IOException, SSLHandshakeException, InterruptedException {
    	
    		SSLSocket socket = null;
    		SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
    		
    		socket = (SSLSocket)factory.createSocket(InetAddress.getByName(loadBalancerHost), LoadBalancer.SERVER_PORT);
			socket.setEnabledProtocols(ENC_PROTOCOLS);
    		socket.setEnabledCipherSuites(socket.getSupportedCipherSuites());

    		PrintWriter pw = new PrintWriter(socket.getOutputStream());
    		
    		Scanner scanner = new Scanner(new BufferedReader(new InputStreamReader(socket.getInputStream())));
    		
    		JSONObject body = new JSONObject();
    		try {
			body.put("host", InetAddress.getLocalHost().getHostAddress());
			body.put("port", this.serverClientPort);
		} catch (JSONException e) {
			e.printStackTrace();
		}
    		
    		        		
    		pw.println(Message.MessageType.NEW_SERVER.description + " " + body.toString());
    		pw.flush();
    		
    		if(scanner.hasNextLine()){
    		    System.out.println(scanner.nextLine());
    		}
    		//System.out.println(scanner.nextLine());
    		scanner.close();
    		pw.close();
    				
    }
    
    
    
	
}