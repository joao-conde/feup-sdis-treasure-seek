package main;

import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import javax.rmi.ssl.SslRMIClientSocketFactory;

import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import communications.Message;
import communications.ReplyMessage;
import communications.ReplyMessage.ReplyMessageStatus;
import controller.UserController;
import model.Treasure;
import model.User;
import util.DuplicatedAppServer;
import util.NonExistentAppServer;
import util.ParseMessageException;
import util.Utils;


public class AppServer{

	public static String[] ENC_PROTOCOLS = new String[] {"TLSv1.2"};
	public static String[] ENC_CYPHER_SUITES = new String[] {"TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256"};
		
    private static final int REGISTRY_PORT = 1099;
    private static final int TIME_OUT = 2000;

	//private static String[] dbServerIPs;
	
	private ExecutorService threadPool = Executors.newFixedThreadPool(20);
	public int serverClientPort;
	
	private UserController userController;
	private DBOperations dbOperations;
	
	private String dbServerHostAddres;
	private String lbHostAddress;

  
    public static void main(String[] args) throws InterruptedException {
		
        String loadBalancerHost = args[0];
        int clientServerPort = Integer.parseInt(args[1]);
        
        AppServer appServer = new AppServer(loadBalancerHost, clientServerPort);
            
		Runtime.getRuntime().addShutdownHook(new Thread(new AppServer.CloseAppServer(appServer)));

        appServer.receiveCalls();

    }
    
    
    private DBOperations getDBOperations() throws RemoteException, NotBoundException, UnknownHostException {
    	
    		Registry registry = LocateRegistry.getRegistry(
    				InetAddress.getLocalHost().getHostName(), REGISTRY_PORT,
                new SslRMIClientSocketFactory());
         
    		dbOperations = (DBOperations) registry.lookup("db_1");
    		return dbOperations;

    }
    
	
    public AppServer(String loadBalancerHost, int serverClientPort) throws InterruptedException {
		
    	Utils.setSecurityProperties();

		this.serverClientPort = serverClientPort;
		this.lbHostAddress = loadBalancerHost;
		
		try {
			this.dbServerHostAddres = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e1) {
			System.out.println(e1.getLocalizedMessage());
		}
		
	
		try {
			this.dbOperations = getDBOperations();
				
		}  catch(UnknownHostException e) {
		
			System.out.println("App Server could not connect to DB Server on host " + dbServerHostAddres + ":" + REGISTRY_PORT);
			System.exit(1);
			
		}
    		
    		catch ( IOException e) {
			
    			System.out.println("App Server could not connect to DB Server on host " + dbServerHostAddres + ":" + REGISTRY_PORT);
    			System.exit(1);
    		} 
    		
    		catch (NotBoundException e) {
			
    			System.out.println("App Server could not connect to remote rmi object");
    			System.exit(1);
    		} 
    		
    		
    		try {
    			announceToLB(loadBalancerHost, Message.MessageType.NEW_SERVER);
			} catch (SSLHandshakeException e) {
				System.out.println("App Server could not perform SSL handshake with Load Balancer on host " + lbHostAddress + ":" + LoadBalancer.SERVER_PORT);
				System.exit(1);
			} catch (IOException | ParseMessageException | DuplicatedAppServer | NonExistentAppServer | JSONException e) {
				System.out.println("App Server could not connect to Load Balancer on host " + lbHostAddress + ":" + LoadBalancer.SERVER_PORT);
				System.exit(1);
			}
    		
    		 	
    		this.userController = new UserController(dbOperations);		
    	
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
				PrintWriter pw = new PrintWriter(this.socket.getOutputStream(), true);
				BufferedReader br = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
				Scanner scanner = new Scanner(br);
				
				if(scanner.hasNextLine()) {
					
					String messageString = scanner.nextLine();
										
					Message messageReceived = Message.parseMessage(messageString);
					String reply = this.handleMessage(messageReceived);
					
					pw.println(reply);
					
					System.out.println(messageReceived);
					
				}
				
			
				scanner.close();
				pw.close();
								
			} catch (IOException | ParseMessageException | JSONException e) {
				e.printStackTrace();
				System.out.println(e.getMessage());
			}
			
		}
		
		private String handleMessage(Message receivedMessage) throws JSONException {
			
			switch (receivedMessage.getHeader().getMessageType()) {
				
				case LOGIN:
					
					System.out.println("LOGIN APP SERVER");
					
					User user = userController.loginUser(receivedMessage.getBody().getString("token"));
					
					if(user != null) {
						
						JSONArray body = new JSONArray();
						
						JSONObject userJson = user.toJSON();
						body.put(userJson);
						
						System.out.println("User " + (String) user.getValue("name")  + " logged in");
						
						ArrayList<Treasure> allTreasures = userController.getAllTreasures();
						JSONArray allTreasuresJSONArray = new JSONArray();
						
						for(Treasure treasure : allTreasures) {
							
							allTreasuresJSONArray.put(treasure.toJSON());
							
						}
						
						body.put(allTreasuresJSONArray);
						
						return ReplyMessage.buildResponseMessage(ReplyMessageStatus.OK, body);
					}
						
					
					return ReplyMessage.buildResponseMessage(ReplyMessageStatus.UNAUTHORIZED);
					
				case LOGOUT:
					
					boolean result = userController.logoutUser(receivedMessage.getBody().getLong("id"), receivedMessage.getBody().getString("token"));
					
					if(result) {
						
						String name = "";
						
						if(receivedMessage.getBody().has("name"))
							name = receivedMessage.getBody().getString("name");
						
						System.out.println("User " + name + " logged out");
						return ReplyMessage.buildResponseMessage(ReplyMessageStatus.OK);
						
						
					}
						
					
					return ReplyMessage.buildResponseMessage(ReplyMessageStatus.UNAUTHORIZED);
					
					
						
				default:
					return ReplyMessage.buildResponseMessage(ReplyMessageStatus.BAD_REQUEST);
			}
			
			
		}
		
		
    	
    }
    
    
    public void announceToLB(String loadBalancerHost, Message.MessageType message) throws UnknownHostException, IOException, SSLHandshakeException, InterruptedException, SocketTimeoutException, ParseMessageException, JSONException, DuplicatedAppServer, NonExistentAppServer {
    	
    		SSLSocket socket = null;
    		SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
    		

    		socket = (SSLSocket)factory.createSocket(InetAddress.getByName(loadBalancerHost), LoadBalancer.SERVER_PORT);
    		socket.setEnabledProtocols(ENC_PROTOCOLS);
    		socket.setEnabledCipherSuites(socket.getSupportedCipherSuites());

    		PrintWriter pw = new PrintWriter(socket.getOutputStream(), true);
    		Scanner scanner = new Scanner(new BufferedReader(new InputStreamReader(socket.getInputStream())));
    		
    		JSONObject body = new JSONObject();
    		try {
				body.put("host", socket.getLocalAddress().getHostAddress());
    			body.put("port", this.serverClientPort);
			} catch (JSONException e) {
				e.printStackTrace();
			}
	    		
       		
    		pw.println(message.description + " " + body.toString());
    		
    		socket.setSoTimeout(TIME_OUT);
    		
    		ReplyMessageStatus reply = null;
    		
    		if(scanner.hasNextLine()){
    		    reply = ReplyMessage.parseResponse(scanner.nextLine());
    		}
    		
    		if(reply != ReplyMessageStatus.OK) {
    			scanner.close();
    			
    			switch (message) {
				case NEW_SERVER:
					throw new DuplicatedAppServer();
				case SHUTDOWN_SERVER:
					throw new NonExistentAppServer();
				default:
					throw new NonExistentAppServer(); //TODO: All possible errors
				}
    		}
    			

    		scanner.close();
    		pw.close();
    				
    }
    
    static class CloseAppServer implements Runnable{
    	
    	AppServer appServer;
    	
		public CloseAppServer(AppServer appServer) {
			this.appServer = appServer;
		}

		public void run() {
			try {
    			appServer.announceToLB(appServer.lbHostAddress, Message.MessageType.SHUTDOWN_SERVER);
			} catch (SSLHandshakeException e) {
				System.out.println("App Server could not perform SSL handshake with Load Balancer on host " + appServer.lbHostAddress + ":" + LoadBalancer.SERVER_PORT);
				System.exit(1);
			} catch (IOException | ParseMessageException | DuplicatedAppServer | NonExistentAppServer | JSONException e) {
				System.out.println("App Server could not connect to Load Balancer on host " + appServer.lbHostAddress + ":" + LoadBalancer.SERVER_PORT);
				System.exit(1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		};
	}
}















