package main;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.sql.SQLException;
import javax.rmi.ssl.SslRMIClientSocketFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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
import controller.Controller;
import model.Model;
import model.Model.ModelType;
import model.Treasure;
import model.User;
import util.DuplicatedAppServer;
import util.NonExistentAppServer;
import util.NotAuthorizedException;
import util.ParseMessageException;
import util.ResourceNotFoundException;
import util.Utils;
import util.Utils.Pair;

public class AppServer {

	public static String[] ENC_PROTOCOLS = new String[] { "TLSv1.2" };
	public static String[] ENC_CYPHER_SUITES = new String[] { "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256" };


	private static final int CLIENT_NOTIFICATION_PORT = 4012;
	private static final int TIME_OUT = 4000;
	public static final String DB_SERVER_OBJECT_NAME = "dbServerObject";

	private ExecutorService threadPool = Executors.newFixedThreadPool(20);

	private Controller controller;

	private String lbHostAddress;
	private String appServerHost;
	public int clientServerPort;
	private ArrayList<String> dbServerHostAddresses = new ArrayList<>();
//	private ArrayList<DBOperations> dbRemoteObjects = new ArrayList<>();
	public int dbRemoteIndex = -1;

	public static void main(String[] args)
			throws InterruptedException, ExecutionException, TimeoutException, RemoteException {

				
		if(Arrays.asList(args).indexOf("--help") != -1) {
			System.out.println(usage());
			System.exit(1);
		}
		
		if(args.length < 3) {
			System.err.println("Invalid number of arguments.");
		}
		
		Utils.bindParamenter(args, "--help", usage(), usage());
		String loadBalancerHost = Utils.bindParamenter(args, "-lb", null, usage());
		String serverAndPort = Utils.bindParamenter(args, "-sp", null, usage());
		String appServerHost = serverAndPort.substring(0, serverAndPort.indexOf(":"));
		int clientServerPort = Integer.parseInt(serverAndPort.substring(serverAndPort.indexOf(":") + 1));
		
		ArrayList<String> dbServersAddresses = Utils.bindMultiParamenter(args, "-dbs", null, usage());
		
		AppServer appServer = new AppServer(loadBalancerHost, appServerHost, clientServerPort, dbServersAddresses);

		Runtime.getRuntime().addShutdownHook(new Thread(new AppServer.CloseAppServer(appServer)));

		System.out.println(Utils.squaredFrame("App server running..."));
		System.out.println(" -> Load Balancer IP Address: " + loadBalancerHost);
		System.out.println(" -> App Server IP Address and Port: " + serverAndPort);
		System.out.println(" -> DB Servers Registry IP Address: ");
		for (int i = 0; i < dbServersAddresses.size(); i++) {
			System.out.println("\t- " + dbServersAddresses.get(i));
		}
		appServer.receiveCalls();

	}


	public static String usage() {
		
		ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
		PrintWriter out = new PrintWriter(outBuffer);
		
		out.println("Usage:");
		out.println("run_app_server.sh <args>:");
		out.println("\t<args>:");
		out.println("\t[required] -lb <load balancer ip address> ==> Defines Load Balancer IP Address");
		out.println("\t[required] -sp <ip server>:<port> ==> Defines IP Address and Port where Cliente will be served");
		out.println("\t[required] -dbs <IP_db1> <IP_db2> ... ==> Defines the IP Addresses of DB registries");
		out.println("\n\t--help ==> Help");
		out.close();
		
		return outBuffer.toString();
		
	}

	public AppServer(String loadBalancerHost, String appServerHost, int clientServerPort, ArrayList<String> dbServersAddresses) throws InterruptedException, RemoteException {

		Utils.setSecurityProperties(false);

		this.lbHostAddress = loadBalancerHost;
		this.appServerHost = appServerHost;
		this.clientServerPort = clientServerPort;
		this.dbServerHostAddresses = dbServersAddresses;
		this.controller = new Controller(dbServerHostAddresses);
		
//		for (int i = 0; i < dbServerHostAddresses.size(); i++) {
//			Registry registry = LocateRegistry.getRegistry(dbServerHostAddresses.get(i), Registry.REGISTRY_PORT,
//
//					new SslRMIClientSocketFactory());
//
//			for (int j = 0; j < registry.list().length; j++) {
//				try {
//					this.dbRemoteObjects.add((DBOperations) registry.lookup(registry.list()[j]));
//				} catch (NotBoundException e1) {
//					System.err.println("DB with name: " + registry.list()[j] + " at host "
//							+ dbServerHostAddresses.get(i) + " doesn't exist");
//				}
//			}
//		}

		try {
			announceToLB();
		} catch (SSLHandshakeException e) {
			System.out.println("App Server could not perform SSL handshake with Load Balancer on host " + lbHostAddress
					+ ":" + LoadBalancer.SERVER_PORT);
			System.exit(1);
		} catch (IOException | ParseMessageException | DuplicatedAppServer | NonExistentAppServer | JSONException e) {
			System.out.println("App Server could not connect to Load Balancer on host " + lbHostAddress + ":"
					+ LoadBalancer.SERVER_PORT);
			System.exit(1);
		}

	}

	private void receiveCalls() throws InterruptedException, ExecutionException, TimeoutException {

		SSLServerSocket serverSocket = null;
		SSLServerSocketFactory factory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();

		try {

			serverSocket = (SSLServerSocket) factory.createServerSocket(clientServerPort);
			serverSocket.setNeedClientAuth(false);
			serverSocket.setEnabledProtocols(ENC_PROTOCOLS);
			serverSocket.setEnabledCipherSuites(serverSocket.getSupportedCipherSuites());

			while (true) {

				try {

					SSLSocket socket = (SSLSocket) serverSocket.accept();

					handleRequest(socket);

				} catch (IOException e) {
					e.printStackTrace();
				}

			}
		}

		catch (IOException e) {

			e.printStackTrace();
			System.out.println(e.getMessage());

		}

	}

	public void handleRequest(SSLSocket socket) throws InterruptedException, ExecutionException, IOException {
		HandleClientRequest callable = new HandleClientRequest(socket);
		Future<String> handler = threadPool.submit(callable);

		try {
			handler.get(TIME_OUT, TimeUnit.MILLISECONDS);
		} catch (TimeoutException e) {
			System.out.println("TimeOutException handling the request");
			handler.cancel(true);

			PrintWriter pw = new PrintWriter(socket.getOutputStream());
			pw.write(ReplyMessage.buildResponseMessage(ReplyMessageStatus.BAD_REQUEST));
			pw.close();

		}

	}

	class HandleClientRequest implements Callable<String> {

		SSLSocket socket;

		public HandleClientRequest(SSLSocket socket) {
			this.socket = socket;
		}

		@Override
		public String call() throws Exception {

			try {
				PrintWriter pw = new PrintWriter(this.socket.getOutputStream(), true);
				BufferedReader br = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
				Scanner scanner = new Scanner(br);

				if (scanner.hasNextLine()) {

					String messageString = scanner.nextLine();
					
					//System.out.println("STRING MSG\n" + messageString);
					Message messageReceived = Message.parseMessage(messageString);
					
					System.out.println("\n\n\n\n\n----------MESSAGE RECEIVED AT APP SERVER----------\n\n" + messageReceived + "\n\n");
					String reply = this.handleMessage(messageReceived);
					
					pw.println(reply);
				}

				scanner.close();
				pw.close();

			} catch (IOException | ParseMessageException | JSONException e) {
				//e.printStackTrace();
				System.out.println(e.getMessage());
				
			}
			return "OK";
		}

		private String handleMessage(Message receivedMessage)
				throws JSONException, RemoteException, ResourceNotFoundException, NotAuthorizedException, SQLException {

			switch (receivedMessage.getHeader().getMessageType()) {

			case LOGIN:

				User user = controller.loginUser(receivedMessage.getBody(), chooseDB());

				if (user != null) {

					Pair<ArrayList<Treasure>, ArrayList<Treasure>> allTreasures = controller
							.getAllTreasures((long) user.getValue("id"), chooseDB());

					JSONArray foundTreasuresJSON = new JSONArray();

					for (Treasure treasure : allTreasures.value) {
						foundTreasuresJSON.put(treasure.toJSON());
					}

					user.setValue("foundTreasures", foundTreasuresJSON);

					JSONArray body = new JSONArray();

					JSONObject userJson = user.toJSON();
					body.put(userJson);

					JSONArray allTreasuresJSONArray = new JSONArray();

					for (Treasure treasure : allTreasures.key) {
						allTreasuresJSONArray.put(treasure.toJSON());
					}

					body.put(allTreasuresJSONArray);

					return ReplyMessage.buildResponseMessage(ReplyMessageStatus.OK, body);
				}

				return ReplyMessage.buildResponseMessage(ReplyMessageStatus.UNAUTHORIZED);

			case LOGOUT:

				boolean result = controller.logoutUser(receivedMessage.getBody().getLong("id"),
						receivedMessage.getBody().getString("token"), chooseDB());

				if (result)
					return ReplyMessage.buildResponseMessage(ReplyMessageStatus.OK);
				
				return ReplyMessage.buildResponseMessage(ReplyMessageStatus.UNAUTHORIZED);

			case CREATE:

				ModelType type = receivedMessage.getHeader().getResource().get(0).key;

				Pair<Boolean, Treasure> createResult = null;

				if (type == Model.ModelType.FOUND_TREASURE) {

					try {
						createResult = controller.validateTreasure(receivedMessage.getBody().getInt("treasureId"),
								receivedMessage.getBody().getString("answer"),
								receivedMessage.getBody().getString("token"),
								receivedMessage.getBody().getLong("userId"), chooseDB());
					} catch (ResourceNotFoundException e) {

						ReplyMessage.buildResponseMessage(ReplyMessageStatus.RESOURCE_NOT_FOUND);

					} catch (NotAuthorizedException e) {
						ReplyMessage.buildResponseMessage(ReplyMessageStatus.UNAUTHORIZED);

					} catch (SQLException | RemoteException e) {

						ReplyMessage.buildResponseMessage(ReplyMessageStatus.BAD_REQUEST);

					}

					JSONArray jsonArray = new JSONArray();
					JSONObject json = new JSONObject();
					json.put("result", createResult.key);
					json.put("challenge", createResult.value.getValue("challenge"));
					json.put("id", createResult.value.getValue("id"));
					json.put("answer", createResult.value.getValue("answer"));
					jsonArray.put(json);

					return ReplyMessage.buildResponseMessage(ReplyMessageStatus.OK, jsonArray);

				}

				else if (type == Model.ModelType.TREASURE) {
					boolean inserted = false;

					try {
						inserted = controller.createTreasure(receivedMessage.getBody(), chooseDB());
					} catch (ResourceNotFoundException e) {
						ReplyMessage.buildResponseMessage(ReplyMessageStatus.RESOURCE_NOT_FOUND);
					} catch (NotAuthorizedException e) {
						ReplyMessage.buildResponseMessage(ReplyMessageStatus.UNAUTHORIZED);
					} catch (SQLException | RemoteException e) {
						ReplyMessage.buildResponseMessage(ReplyMessageStatus.BAD_REQUEST);
					}

					if (inserted) {

						try {
							ArrayList<String> addresses = controller.getSubscribedUsersAddresses(chooseDB());

							for (String address : addresses) {

								threadPool.execute(
										new NotifyClient(address, receivedMessage.getBody().getString("description")));
							}

						} catch (RemoteException e) {
							e.printStackTrace();
						} catch (SQLException e) {
							e.printStackTrace();
						}

						return ReplyMessage.buildResponseMessage(ReplyMessageStatus.OK);
					} else
						return ReplyMessage.buildResponseMessage(ReplyMessageStatus.BAD_REQUEST);
				}
				break;
			case RETRIEVE:

				ModelType retrieveType = receivedMessage.getHeader().getResource().get(0).key;
				int id = receivedMessage.getHeader().getResource().get(0).value;

				if (retrieveType == Model.ModelType.TREASURE && id == -1) {
					
					JSONObject body = receivedMessage.getBody();
										
					Pair<ArrayList<Treasure>, ArrayList<Treasure>> allTreasures = controller
							.getAllTreasures(body.getLong("userId"), chooseDB(), body.getString("token"));
					
					JSONArray treasures = new JSONArray();
					
					JSONArray foundTreasures = new JSONArray();
					
					for (Treasure treasure : allTreasures.value) {
						foundTreasures.put(treasure.toJSON());
					}			

					JSONArray notFoundTreasures = new JSONArray();

					for (Treasure treasure : allTreasures.key) {
						notFoundTreasures.put(treasure.toJSON());
					}
					
					treasures.put(notFoundTreasures);
					treasures.put(foundTreasures);
					
					return ReplyMessage.buildResponseMessage(ReplyMessageStatus.OK, treasures);
				}

				else if(retrieveType == Model.ModelType.USER && id == -1){

					JSONObject body = receivedMessage.getBody();
					
					ArrayList<Pair<User, Integer>> ranking = controller
							.getRanking(body.getLong("userId"), chooseDB(), body.getString("token"));
					
					JSONArray rankingJSONArray = new JSONArray();
										
					for (Pair<User, Integer> userRanking : ranking) {
						JSONObject jsonObj = userRanking.key.toJSON();
						jsonObj.put("score", userRanking.value);
						rankingJSONArray.put(jsonObj);
					}

					return ReplyMessage.buildResponseMessage(ReplyMessageStatus.OK, rankingJSONArray);

				}

				return ReplyMessage.buildResponseMessage(ReplyMessageStatus.BAD_REQUEST);

			default:
				return ReplyMessage.buildResponseMessage(ReplyMessageStatus.BAD_REQUEST);
			}
			return null;
		}

	}

	public void announceToLB() throws UnknownHostException, IOException, SSLHandshakeException, InterruptedException,
			SocketTimeoutException, ParseMessageException, JSONException, DuplicatedAppServer, NonExistentAppServer {

		SSLSocket socket = null;
		SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();

		socket = (SSLSocket) factory.createSocket(InetAddress.getByName(this.lbHostAddress), LoadBalancer.SERVER_PORT);
		socket.setEnabledProtocols(ENC_PROTOCOLS);
		socket.setEnabledCipherSuites(socket.getSupportedCipherSuites());

		PrintWriter pw = new PrintWriter(socket.getOutputStream(), true);
		Scanner scanner = new Scanner(new BufferedReader(new InputStreamReader(socket.getInputStream())));

		Message.MessageType message = Message.MessageType.NEW_SERVER;

		JSONObject body = new JSONObject();
		try {
			body.put("host", this.appServerHost);
			body.put("port", this.clientServerPort);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		pw.println(message.description + " " + body.toString());

		socket.setSoTimeout(TIME_OUT);

		ReplyMessageStatus reply = null;

		if (scanner.hasNextLine()) {
			reply = ReplyMessage.parseResponse(scanner.nextLine());
		}

		if (reply != ReplyMessageStatus.OK) {
			scanner.close();
			throw new DuplicatedAppServer();
		}

		scanner.close();
		pw.close();

	}

	public void kill() throws IOException {

		SSLSocket socket = null;
		SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();

		socket = (SSLSocket) factory.createSocket();
		socket.setEnabledProtocols(ENC_PROTOCOLS);
		socket.setEnabledCipherSuites(socket.getSupportedCipherSuites());

		SocketAddress socketAddr = new InetSocketAddress(InetAddress.getByName(this.lbHostAddress),
				LoadBalancer.SERVER_PORT);
		socket.connect(socketAddr, TIME_OUT);

		PrintWriter pw = new PrintWriter(socket.getOutputStream(), true);
		Scanner scanner = new Scanner(new BufferedReader(new InputStreamReader(socket.getInputStream())));

		Message.MessageType message = Message.MessageType.SHUTDOWN_SERVER;

		JSONObject body = new JSONObject();
		try {
			body.put("host", this.appServerHost);
			body.put("port", this.clientServerPort);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		pw.println(message.description + " " + body.toString());
		scanner.close();
		pw.close();

	}

	public static class CloseAppServer implements Runnable {

		AppServer appServer;

		public CloseAppServer(AppServer appServer) {
			this.appServer = appServer;
		}

		public void run() {
			try {
				appServer.kill();
			} catch (SSLHandshakeException e) {
				System.out.println("App Server could not perform SSL handshake with Load Balancer on host "
						+ appServer.lbHostAddress + ":" + LoadBalancer.SERVER_PORT);
				System.exit(1);
			} catch (IOException e) {
				System.out.println("App Server could not connect to Load Balancer on host " + appServer.lbHostAddress
						+ ":" + LoadBalancer.SERVER_PORT);
				System.exit(1);
			}
		};
	}

	public static class NotifyClient implements Runnable {

		private String address;
		private String treasureDescription;
		private Socket socket;

		public NotifyClient(String address, String treasure) {
			this.address = address;
			this.treasureDescription = treasure;
			this.socket = new Socket();
		}

		@Override
		public void run() {
			System.out.println("\n------Notifying client (separate thread)------\n" + this.address + "\n");

			SocketAddress socketAddress = new InetSocketAddress(address, CLIENT_NOTIFICATION_PORT);
			try {
				socket.connect(socketAddress, TIME_OUT);
				PrintWriter pw = new PrintWriter(socket.getOutputStream(), true);
				pw.println(treasureDescription);
				pw.close();

			} catch (IOException e) {
				e.printStackTrace();
			}

		}

	}

	public DBOperations chooseDB() {
		dbRemoteIndex++;
		int counter = 0;

		for (int t = 0; t < 2; t++) {

			for (int i = 0; i < dbServerHostAddresses.size(); i++) {
				Registry registry = null;
				try {
					registry = LocateRegistry.getRegistry(dbServerHostAddresses.get(i), Registry.REGISTRY_PORT,
							new SslRMIClientSocketFactory());				

					String[] objList = registry.list();
					for (int j = 0; j < objList.length; j++) {
						DBOperations obj;
						try {
							obj = (DBOperations) registry.lookup(objList[j]);
						} catch (NotBoundException e) {
							e.toString();
							continue;
						}

						if(dbRemoteIndex == counter) {
							return obj;
						}
						else {
							counter++;
						}
					}
				} catch (RemoteException e) {
					e.toString();
					continue;
				}
			}
			dbRemoteIndex = 0;
			counter = 0;
		}
		System.err.println("No available DB obj.");
		return null;
		
	}

}
