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
import java.rmi.ConnectException;
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
import controller.UserController;
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
	private static final int TIME_OUT = 2000;
	public static final String DB_SERVER_OBJECT_NAME = "dbServerObject";

	private ExecutorService threadPool = Executors.newFixedThreadPool(20);

	private UserController userController;

	private String lbHostAddress;
	private String appServerHost;
	public int clientServerPort;
	private ArrayList<String> dbServerHostAddresses = new ArrayList<>();
	private ArrayList<DBOperations> dbRemoteObjects = new ArrayList<>();
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
		
		
		String loadBalancerHost = args[0];
		String appServerHost = args[1].substring(0, args[1].indexOf(":"));
		int clientServerPort = Integer.parseInt(args[1].substring(args[1].indexOf(":") + 1));
		String[] dbServersAddresses = new String[args.length - 2];
		System.arraycopy(args, 2, dbServersAddresses, 0, args.length - 2);

		AppServer appServer = new AppServer(loadBalancerHost, appServerHost, clientServerPort, dbServersAddresses);

		Runtime.getRuntime().addShutdownHook(new Thread(new AppServer.CloseAppServer(appServer)));

		appServer.receiveCalls();

	}


	public static String usage() {
		
		ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
		PrintWriter out = new PrintWriter(outBuffer);
		
		out.println("Usage:");
		out.println("run_app_server.sh <args>:");
		out.println("\t<args>:");
		out.println("\t--help ==> Help");
		out.println("\t-lb <load balancer ip address> ==> Defines localhost IP Address");
		out.println("\tFALTA CENAS");
		out.close();
		
		return outBuffer.toString();
		
	}

	public AppServer(String loadBalancerHost, String appServerHost, int clientServerPort, String[] dbServersAddresses) throws InterruptedException, RemoteException {

		Utils.setSecurityProperties(false);

		this.lbHostAddress = loadBalancerHost;
		this.appServerHost = appServerHost;
		this.clientServerPort = clientServerPort;
		this.dbServerHostAddresses = new ArrayList<>(Arrays.asList(dbServersAddresses));
		this.userController = new UserController(dbServerHostAddresses);
		
		for (int i = 0; i < dbServerHostAddresses.size(); i++) {
			Registry registry = LocateRegistry.getRegistry(dbServerHostAddresses.get(i), Registry.REGISTRY_PORT,

					new SslRMIClientSocketFactory());

			for (int j = 0; j < registry.list().length; j++) {
				try {
					this.dbRemoteObjects.add((DBOperations) registry.lookup(registry.list()[j]));
				} catch (NotBoundException e1) {
					System.err.println("DB with name: " + registry.list()[j] + " at host "
							+ dbServerHostAddresses.get(i) + " doesn't exist");
				}
			}
		}

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
		System.out.println("socket: " + socket);
		HandleClientRequest callable = new HandleClientRequest(socket);
		System.out.println("callable: " + callable);
		Future<String> handler = threadPool.submit(callable);
		System.out.println("handler: " + handler);

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

					System.out.println("\n\nMessage Received: " + messageString + "\n\n");

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
			return "OK";
		}

		private String handleMessage(Message receivedMessage)
				throws JSONException, RemoteException, ResourceNotFoundException, NotAuthorizedException, SQLException {

			switch (receivedMessage.getHeader().getMessageType()) {

			case LOGIN:

				System.out.println("LOGIN APP SERVER");

				User user = userController.loginUser(receivedMessage.getBody(), chooseDB());

				if (user != null) {

					Pair<ArrayList<Treasure>, ArrayList<Treasure>> allTreasures = userController
							.getAllTreasures((long) user.getValue("id"), chooseDB());

					JSONArray foundTreasuresJSON = new JSONArray();

					for (Treasure treasure : allTreasures.value) {
						foundTreasuresJSON.put(treasure.toJSON());
					}

					user.setValue("foundTreasures", foundTreasuresJSON);

					JSONArray body = new JSONArray();

					JSONObject userJson = user.toJSON();
					body.put(userJson);

					System.out.println("User " + (String) user.getValue("name") + " logged in");

					JSONArray allTreasuresJSONArray = new JSONArray();

					for (Treasure treasure : allTreasures.key) {
						allTreasuresJSONArray.put(treasure.toJSON());
					}

					body.put(allTreasuresJSONArray);

					return ReplyMessage.buildResponseMessage(ReplyMessageStatus.OK, body);
				}

				return ReplyMessage.buildResponseMessage(ReplyMessageStatus.UNAUTHORIZED);

			case LOGOUT:

				boolean result = userController.logoutUser(receivedMessage.getBody().getLong("id"),
						receivedMessage.getBody().getString("token"), chooseDB());

				if (result) {

					String name = "";

					if (receivedMessage.getBody().has("name"))
						name = receivedMessage.getBody().getString("name");

					System.out.println("User " + name + " logged out");
					return ReplyMessage.buildResponseMessage(ReplyMessageStatus.OK);

				}

				return ReplyMessage.buildResponseMessage(ReplyMessageStatus.UNAUTHORIZED);

			case CREATE:

				ModelType type = receivedMessage.getHeader().getResource().get(0).key;

				Pair<Boolean, Treasure> createResult = null;

				if (type == Model.ModelType.FOUND_TREASURE) {

					try {
						createResult = userController.validateTreasure(receivedMessage.getBody().getInt("treasureId"),
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

					String name = "";
					if (receivedMessage.getBody().has("name") && createResult.key)
						name = receivedMessage.getBody().getString("name");

					System.out.println("User " + name + " found a treasure");

					JSONArray jsonArray = new JSONArray();
					JSONObject json = new JSONObject();
					json.put("result", createResult.key);
					json.put("challenge", createResult.value.getValue("challenge"));
					json.put("id", createResult.value.getValue("id"));
					json.put("answer", createResult.value.getValue("answer"));
					jsonArray.put(json);

					return ReplyMessage.buildResponseMessage(ReplyMessageStatus.OK, jsonArray);

				}

				if (type == Model.ModelType.TREASURE) {
					boolean inserted = false;

					try {
						inserted = userController.createTreasure(receivedMessage.getBody(), chooseDB());
					} catch (ResourceNotFoundException e) {
						ReplyMessage.buildResponseMessage(ReplyMessageStatus.RESOURCE_NOT_FOUND);
					} catch (NotAuthorizedException e) {
						ReplyMessage.buildResponseMessage(ReplyMessageStatus.UNAUTHORIZED);
					} catch (SQLException | RemoteException e) {
						ReplyMessage.buildResponseMessage(ReplyMessageStatus.BAD_REQUEST);
					}

					System.out.println("TREASURE " + inserted);

					if (inserted) {

						try {
							ArrayList<String> addresses = userController.getSubscribedUsersAddresses(chooseDB());

							for (String address : addresses) {

								threadPool.execute(
										new NotifyClient(address, receivedMessage.getBody().getString("description")));
							}

						} catch (RemoteException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (SQLException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

						return ReplyMessage.buildResponseMessage(ReplyMessageStatus.OK);
					} else
						return ReplyMessage.buildResponseMessage(ReplyMessageStatus.BAD_REQUEST);
				}

			case RETRIEVE:

				ModelType retrieveType = receivedMessage.getHeader().getResource().get(0).key;
				int id = receivedMessage.getHeader().getResource().get(0).value;

				if (retrieveType == Model.ModelType.TREASURE && id == -1) {
					
					System.out.println("getting all treasures");

					JSONObject body = receivedMessage.getBody();
					
					System.out.println("JSONOBJECT: " + body); 
					
					Pair<ArrayList<Treasure>, ArrayList<Treasure>> treasures = userController
							.getAllTreasures(body.getLong("userId"), chooseDB(), body.getString("token"));
					
					JSONArray treasuresJSONArray = new JSONArray();
					
					System.out.println("TREASURES " + treasures);
					
					//TODO: check index out of range possible wrong json
					for (Treasure treasure : treasures.value) {
						treasuresJSONArray.put(treasure.toJSON());
					}


					return ReplyMessage.buildResponseMessage(ReplyMessageStatus.OK, treasuresJSONArray);
				}

				return ReplyMessage.buildResponseMessage(ReplyMessageStatus.BAD_REQUEST);

			default:
				return ReplyMessage.buildResponseMessage(ReplyMessageStatus.BAD_REQUEST);
			}

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
			System.out.println("NOTIFY CLIENT THREAD " + this.address);

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
		// dbRemoteIndex++;
		// if (dbRemoteIndex == dbRemoteObjects.size()) {
		// 	dbRemoteIndex = 0;
		// }
		// System.out.println("Incremented dbRemoteIndex: " + dbRemoteIndex);

		// return dbRemoteObjects.get(dbRemoteIndex);
		dbRemoteIndex++;
		int counter = 0;

		for (int t = 0; t < 2; t++) {
			System.out.println("chooseDB: " + dbRemoteIndex);
			for (int i = 0; i < dbServerHostAddresses.size(); i++) {
				Registry registry = null;
				try {
					System.out.println("getRegistry: " + dbServerHostAddresses.get(i));
					registry = LocateRegistry.getRegistry(dbServerHostAddresses.get(i), Registry.REGISTRY_PORT,
							new SslRMIClientSocketFactory());				
					System.out.println("registry.list(): " + dbServerHostAddresses.get(i));
					String[] objList = registry.list();
					for (int j = 0; j < objList.length; j++) {
						DBOperations obj;
						try {
							System.out.println("registry.lookup: " + objList[j]);
							obj = (DBOperations) registry.lookup(objList[j]);
						} catch (NotBoundException e) {
							e.toString();
							continue;
						}
						System.out.println("dbRemoteIndex: " + dbRemoteIndex + " == counter: " + counter);
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
