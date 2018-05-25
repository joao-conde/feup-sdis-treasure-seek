package main;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
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
import java.sql.SQLException;
import javax.rmi.ssl.SslRMIClientSocketFactory;
import java.util.ArrayList;
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

	private static final int REGISTRY_PORT = 1099;
	private static final int TIME_OUT = 2000;
	// private static final int TO_MILLIS = 1000;

	// private static String[] dbServerIPs;

	private ExecutorService threadPool = Executors.newFixedThreadPool(100);

	private UserController userController;
	private DBOperations dbOperations;

	private String lbHostAddress;
	private String appServerHost;
	public int clientServerPort;
	private String dbServerHostAddress;

	public static void main(String[] args)
			throws InterruptedException, ExecutionException, TimeoutException, RemoteException {

		String loadBalancerHost = args[0];
		String appServerHost = args[1].substring(0, args[1].indexOf(":"));
		int clientServerPort = Integer.parseInt(args[1].substring(args[1].indexOf(":") + 1));
		String dbManagerHost = args[2];

		AppServer appServer = new AppServer(loadBalancerHost, appServerHost, clientServerPort, dbManagerHost);

		Runtime.getRuntime().addShutdownHook(new Thread(new AppServer.CloseAppServer(appServer)));

		appServer.receiveCalls();

	}

	public AppServer(String loadBalancerHost, String appServerHost, int clientServerPort, String dbManagerHost)
			throws InterruptedException, RemoteException {

		Utils.setSecurityProperties(false);

		this.lbHostAddress = loadBalancerHost;
		this.appServerHost = appServerHost;
		this.clientServerPort = clientServerPort;
		this.dbServerHostAddress = dbManagerHost;

		Registry registry = LocateRegistry.getRegistry(this.dbServerHostAddress, REGISTRY_PORT,
				new SslRMIClientSocketFactory());

		try {
			this.dbOperations = (DBOperations) registry.lookup("db_manager");
			this.userController = new UserController(dbOperations);
		} catch (NotBoundException e1) {
			System.err.println("DB Manager doesn't exist");
			System.exit(1);
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

		this.userController = new UserController(dbOperations);

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

		private String handleMessage(Message receivedMessage) throws JSONException {

			switch (receivedMessage.getHeader().getMessageType()) {

			case LOGIN:

				System.out.println("LOGIN APP SERVER");

				User user = userController.loginUser(receivedMessage.getBody().getString("token"));

				if (user != null) {

					Pair<ArrayList<Treasure>, ArrayList<Treasure>> allTreasures = userController
							.getAllTreasures((long) user.getValue("id"));

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
						receivedMessage.getBody().getString("token"));

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
								receivedMessage.getBody().getLong("userId"));
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
						inserted = userController.createTreasure(receivedMessage.getBody());
					} catch (ResourceNotFoundException e) {
						ReplyMessage.buildResponseMessage(ReplyMessageStatus.RESOURCE_NOT_FOUND);
					} catch (NotAuthorizedException e) {
						ReplyMessage.buildResponseMessage(ReplyMessageStatus.UNAUTHORIZED);
					} catch (SQLException | RemoteException e) {
						ReplyMessage.buildResponseMessage(ReplyMessageStatus.BAD_REQUEST);
					}

					System.out.println("TREASURE " + inserted);
					
					if(inserted)
						return ReplyMessage.buildResponseMessage(ReplyMessageStatus.OK);
					else
						return ReplyMessage.buildResponseMessage(ReplyMessageStatus.BAD_REQUEST);
				}

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

	static class CloseAppServer implements Runnable {

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
}
