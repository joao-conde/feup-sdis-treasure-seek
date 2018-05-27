package main;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ExportException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.AlreadyBoundException;
import java.rmi.ConnectException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.sql.Statement;
import java.util.Scanner;

import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;

import util.Utils.Pair;
import model.Treasure;
import model.User;
import util.Utils;

public class DBServer extends UnicastRemoteObject implements DBOperations {

	private static final long serialVersionUID = 1L;
	public static String[] ENC_PROTOCOLS = new String[] { "TLSv1.2" };
	public static String[] ENC_CYPHER_SUITES = new String[] { "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256" };
	public static final String DB_SERVER_OBJECT_NAME = "dbServerObject";
	
	private static final String DB_PATH = "../db/";
	
    private String OBJNAME;
    private String DBNAME;
    private String DBURL;
    
    private String localAddress;
    private String recoverDBAddress;
    
    private Connection connection;

          
    protected DBServer(String localAddress, String recoverDBAddress) throws Exception {
		super(0, new SslRMIClientSocketFactory(), new SslRMIServerSocketFactory(null, ENC_PROTOCOLS, false));
		
		this.localAddress = localAddress;
		this.recoverDBAddress = recoverDBAddress;
	}
    
    
	public static void main(String[] args) throws Exception {
		
		Utils.setSecurityProperties(false);  
		
		if(Arrays.asList(args).indexOf("--help") != -1) {
			System.out.println(usage());
			System.exit(1);
		}
		
		String localAddress = Utils.bindParamenter(args, "-lh", InetAddress.getLocalHost().getHostAddress(), usage());
		String recoverDBAddress = Utils.bindParamenter(args, "-r", null, usage());
		DBServer dbServer = new DBServer(localAddress, recoverDBAddress);
		
		dbServer.initRMIInterface();
		System.out.println("DB Server running...");
	}	
	
	public static String usage() {
		
		ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
		PrintWriter out = new PrintWriter(outBuffer);
		
		out.println("Usage:");
		out.println("run_db_server.sh <args>:");
		out.println("\t<args>:");
		out.println("\t--help ==> Help");
		out.println("\t-lh <localhost_address> ==> Defines localhost IP Address");
		out.println("\t-r <recover_db_host_address> ==> Recover DB from IP Address");
		out.close();
		
		return outBuffer.toString();
		
	}


	private void initRMIInterface() throws SQLException, IllegalArgumentException, AlreadyBoundException, IOException{
		
		Registry registry = null;
		try {
			registry = LocateRegistry.createRegistry(Registry.REGISTRY_PORT,
					new SslRMIClientSocketFactory(),
					new SslRMIServerSocketFactory(null,ENC_PROTOCOLS,true));
		}
		catch(ExportException e){
			registry = LocateRegistry.getRegistry(this.localAddress, Registry.REGISTRY_PORT,
					new SslRMIClientSocketFactory());
		}
		
		int i = 0;
		while(true) {
			try {
				
				registry.bind(DB_SERVER_OBJECT_NAME + "_" + i, this);
				break;
			}
			catch(AlreadyBoundException e) {
				i++;
			}
		}
		
		OBJNAME = DB_SERVER_OBJECT_NAME + "_" + i;
		DBNAME = OBJNAME + ".db";

		createConnection();
		Runtime.getRuntime().addShutdownHook(new Thread(new CloseDBServer(registry, OBJNAME)));

	}
	
	private void createConnection() throws SQLException, RemoteException, IOException {
		DBURL = "jdbc:sqlite:../db/" + DBNAME;

		boolean dbFileExists = new File(DB_PATH + DBNAME).exists();

		connection = DriverManager.getConnection(DBURL);
		
		if(recoverDBAddress != null) {
			recoverDBFile();
		}
		else if (!dbFileExists) {
			String schema = "";
			Scanner scanner = new Scanner(new File(DB_PATH + "seed.sql"));

			while (scanner.hasNextLine()) {
				schema += scanner.nextLine();
			}

			scanner.close();

			Statement st = connection.createStatement();
			st.executeUpdate(schema);
			st.close();

		}		
	}


	private void recoverDBFile() throws IOException, FileNotFoundException {
		Registry registry = null;
		DBOperations obj = null;

		try {
			registry = LocateRegistry.getRegistry(recoverDBAddress, Registry.REGISTRY_PORT,
					new SslRMIClientSocketFactory());				
			String[] objList = registry.list();
			for (int j = 0; j < objList.length; j++) {
				try {
					obj = (DBOperations) registry.lookup(objList[j]);
					break;
				} catch (NotBoundException e) {
					e.toString();
					continue;
				}
			}
		} catch (RemoteException e) {
			e.toString();
		}
		
		if(obj == null) {
			System.err.println("Recover Database is not a valid one.");
			System.exit(1);
		}
		
		try (FileOutputStream fos = new FileOutputStream(DB_PATH + DBNAME)) {
		   fos.write(obj.recoverDB());
		   fos.close();
		}
	}

	@Override
	public User insertUser(long id, String email, String token, String name, String address, ArrayList<String> dbServerHostAddresses)
			throws RemoteException {

		try {

			PreparedStatement stmt = connection
					.prepareStatement("INSERT INTO user (id, email, token, name, address) VALUES (?, ?, ?, ?, ?)");

			stmt.setLong(1, id);
			stmt.setString(2, email);
			stmt.setString(3, token);
			stmt.setString(4, name);
			stmt.setString(5, address);
			stmt.executeUpdate();

			System.out.println("User inserted with success on " + this.DBNAME);

			User user = new User();
			user.setValue("id", id);
			user.setValue("email", email);
			user.setValue("token", token);
			user.setValue("name", name);
			user.setValue("admin", false);
			user.setValue("address", address);

			replicateData(FunctionCallType.INSERT_USER, dbServerHostAddresses, new Object[] {id, email, token, name, address});

			return user;

		} catch (SQLException e) {
			System.out.println(e.getMessage());
			return null;
		}

	}

	@Override
	public User getUser(long id) throws RemoteException, SQLException {

		PreparedStatement stmt = connection.prepareStatement("SELECT * from user WHERE id = ?");

		stmt.setLong(1, id);
		ResultSet result = stmt.executeQuery();

		if (!result.next())
			return null;

		User user = new User();
		user.setValue("id", result.getLong(1));
		user.setValue("email", result.getString(2));
		user.setValue("token", result.getString(3));
		user.setValue("name", result.getString(4));
		user.setValue("admin", result.getBoolean(5));
		
		return user;

	}

	@Override
	public boolean updateUser(long id, String token, String address, ArrayList<String> dbServerHostAddresses) throws RemoteException, SQLException {

		try {

			PreparedStatement stmt = connection.prepareStatement("UPDATE user SET token = ? , address = ? WHERE id = ?");

			stmt.setString(1, token);
			stmt.setString(2, address);
			stmt.setLong(3, id);
			stmt.executeUpdate();

			System.out.println("User updated with success on DB " + this.DBNAME);
			
			replicateData(FunctionCallType.UPDATE_USER, dbServerHostAddresses, new Object[] {id, token, address});

			return true;

		} catch (SQLException e) {
			System.out.println(e.getMessage());
			return false;
		}

	}

	@Override
	public ArrayList<Treasure> getAllTreasures() throws RemoteException, SQLException {

		PreparedStatement stmt = connection.prepareStatement("SELECT * from treasure");

		ResultSet result = stmt.executeQuery();
		ArrayList<Treasure> treasures = new ArrayList<>();

		while (result.next()) {

			Treasure treasure = new Treasure();
			treasure.setValue("id", result.getInt(1));
			treasure.setValue("latitude", result.getDouble(2));
			treasure.setValue("longitude", result.getDouble(3));
			treasure.setValue("description", result.getString(4));
			treasures.add(treasure);

		}

		return treasures;

	}

	@Override
	public ArrayList<Pair<User, Integer>> getRanking() throws RemoteException, SQLException{

		PreparedStatement stmt = connection.prepareStatement(
			"SELECT user.*, count(user.id) "+
			"FROM user_treasure" +
			"JOIN user ON user.id = user_treasure.userid" + 
			"GROUP BY user.id" +
			"ORDER BY count(user.id) DESC;"
		);
		
		ResultSet resultSet = stmt.executeQuery();

		ArrayList<Pair<User, Integer>> ranking = new ArrayList<>();

		while(resultSet.next()) {
			
			User user = new User();
			user.setValue("id", resultSet.getInt(1));
			user.setValue("email", resultSet.getString(2));
			user.setValue("token", resultSet.getString(3));
			user.setValue("name", resultSet.getString(4));
			Integer score = resultSet.getInt(7);
						
			ranking.add(new Pair<>(user, score));
				
		}

		return ranking;
	}


	@Override
	public Pair<ArrayList<Treasure>,ArrayList<Treasure>> getAllTreasuresWithFoundInfo(long userId) throws RemoteException, SQLException {
		
		PreparedStatement stmt = connection.prepareStatement(
			"SELECT * FROM (" +
				"SELECT id, latitude, longitude, description, 1 as found, challenge, challengeSolution " + 
				"FROM treasure " + 
				"WHERE (?, treasure.id) " + 
				"IN (select userId, treasureId from user_treasure) " + 
				
				"UNION " + 
				
				"SELECT id, latitude, longitude, description, 0 as found, challenge, challengeSolution " + 
				"FROM treasure " + 
				"WHERE (?, treasure.id) " + 
				"NOT IN (select userId, treasureId from user_treasure) " +
			");"
        );
				
		stmt.setLong(1, userId);
		stmt.setLong(2, userId);
		ResultSet resultSet = stmt.executeQuery();
				
		ArrayList<Treasure> treasures = new ArrayList<>();
		ArrayList<Treasure> foundTreasures = new ArrayList<>();
		
		while(resultSet.next()) {
			
			Treasure treasure = new Treasure();
			treasure.setValue("id", resultSet.getInt(1));
			treasure.setValue("latitude", resultSet.getDouble(2));
			treasure.setValue("longitude", resultSet.getDouble(3));
			treasure.setValue("description", resultSet.getString(4));
						
			if(resultSet.getBoolean(5) == true) {
				
				treasure.setValue("challenge", resultSet.getString(6));
				treasure.setValue("answer", resultSet.getString(7));
				
				foundTreasures.add(treasure);
				
			}
				
			else {
				
				treasure.setValue("challenge", "");
				treasure.setValue("answer", "");
				treasures.add(treasure);
			}
				
		}
		
		Pair<ArrayList<Treasure>,ArrayList<Treasure>> result = new Pair<>(treasures,foundTreasures);					
		return result;
		
	}
	

	@Override
	public Treasure getTreasure(int treasureId) throws RemoteException, SQLException {
				
		PreparedStatement stmt = connection.prepareStatement("SELECT challenge, challengeSolution, id FROM treasure WHERE id = ?");
		stmt.setInt(1, treasureId);
		ResultSet result = stmt.executeQuery();
		
		if (!result.next())
			return null;
		
		Treasure treasure = new Treasure();
		treasure.setValue("challenge", result.getString(1));
		treasure.setValue("answer", result.getString(2));
		treasure.setValue("id", result.getInt(3));
		
		return treasure;
		
	}

	@Override
	public boolean insertFoundTreasure(int treasureId, long userId, ArrayList<String> dbServerHostAddresses) throws RemoteException, SQLException {
		
		PreparedStatement stmt = connection
				.prepareStatement("INSERT INTO user_treasure (userId, treasureId) VALUES (?, ?)");
		
		stmt.setLong(1, userId);
		stmt.setInt(2, treasureId);
		
		stmt.executeUpdate();
		
		System.out.println("Found treasure inserted with success on DB");

			
		replicateData(FunctionCallType.INSERT_FOUND_TREASURE, dbServerHostAddresses, new Object[] {treasureId, userId});
		return true;
	}
	
	@Override
	public boolean insertTreasure(Double latitude, Double longitude, long userCreatorId, String description, String challenge, String challengeSolution, ArrayList<String> dbServerHostAddresses) throws SQLException {
		
		PreparedStatement stmt = connection
				.prepareStatement("INSERT INTO treasure (latitude, longitude, userCreatorId, description, challenge, challengeSolution) VALUES (?, ?, ?, ?, ?, ?)");
		
		stmt.setDouble(1, latitude);
		stmt.setDouble(2, longitude);
		stmt.setLong(3, userCreatorId);
		stmt.setString(4, description);
		stmt.setString(5, challenge);
		stmt.setString(6, challengeSolution);
		
		stmt.executeUpdate();
		
		System.out.println("Treasure inserted with success on DB");

		replicateData(FunctionCallType.INSERT_TREASURE, dbServerHostAddresses, 
						new Object[] {
							latitude,
							longitude, 
							userCreatorId,
							description,
							challenge,
							challengeSolution
						});

		
		return true;
	}
	
	@Override
	public ArrayList<String> getSubscribedUsersAddress() throws SQLException{
		
		ArrayList<String> addresses = new ArrayList<String>();
		
		PreparedStatement stmt = connection.prepareStatement("SELECT address FROM user WHERE address != '' AND  admin=0");
		ResultSet result = stmt.executeQuery();

		while (result.next()) {			
		  String address = result.getString("address");
		  addresses.add(address);
		}
		
		return addresses;	
	}

	private enum FunctionCallType {
		INSERT_USER,
		UPDATE_USER,
		INSERT_FOUND_TREASURE,
		INSERT_TREASURE
	}

	public void replicateData(FunctionCallType functionName, ArrayList<String> dbServerHostAddresses, Object[] args) {

		if(dbServerHostAddresses == null){
			return;
		}

		class ReplicateInsertFoundTreasure implements Runnable{

			@Override
			public void run() {
				
				for (int i = 0; i < dbServerHostAddresses.size(); i++) {
					Registry registry;
					try {
						registry = LocateRegistry.getRegistry(dbServerHostAddresses.get(i), Registry.REGISTRY_PORT,
								new SslRMIClientSocketFactory());
						
						String[] remoteObjects;
						try {
							remoteObjects = registry.list();
						} catch (ConnectException e) {
							continue;
						}
						for (int j = 0; j < remoteObjects.length; j++) {
									
							
							if(remoteObjects[j].equals(OBJNAME) && dbServerHostAddresses.get(i).equals(localAddress)) {
								System.out.println("This is my replicate request, I will ignore it");
								continue;
							}

							System.out.println("Replicating: " + dbServerHostAddresses.get(i) + " at " + remoteObjects[j]);

							DBOperations remoteObj = (DBOperations) registry.lookup(remoteObjects[j]);
							
							switch (functionName) {
							case INSERT_USER:
								System.out.println("Replicating INSERT_USER");

								remoteObj.insertUser(
										(long)   args[0],		//id
										(String) args[1],		//name
										(String) args[2],		//token
										(String) args[3],		//email
										(String) args[4], null);//address

								break;
							case UPDATE_USER:
								System.out.println("Replicating UPDATE_USER");
								remoteObj.updateUser(
										(long)   args[0],		//id
										(String) args[1],		//token
										(String) args[2], null);//address

								break;
							
							case INSERT_FOUND_TREASURE:
								System.out.println("Replicating INSERT_FOUND_TREASURE");
								remoteObj.insertFoundTreasure(
										(int)  args[0], 		//treasureId
										(long) args[1], null);	//userId

								break;

							case INSERT_TREASURE:
								System.out.println("Replicating INSERT_TREASURE");
								remoteObj.insertTreasure(
										(Double) args[0], 		//latitude
										(Double) args[1], 		//longitude
										(long)   args[2], 		//userCreatorId
										(String) args[3], 		//description
										(String) args[4], 		//challenge
										(String) args[5], null);//challengeSolution

								break;

							default:
								break;
							}
						}
						

					} catch (RemoteException | SQLException e) {
						e.printStackTrace();
					} catch (NotBoundException e) {
						e.printStackTrace();
					}
					
				}
			}
		}
		
		new Thread(new ReplicateInsertFoundTreasure()).start();

	}

	@Override
	public byte[] recoverDB() throws RemoteException, IOException {

    	File file = new File(DB_PATH + DBNAME);
    	return Utils.readFile(file);

	}

	static class CloseDBServer implements Runnable { 
		Registry registry; 
		String objName; 
		
		public CloseDBServer(Registry registry, String objName) { 
			this.registry = registry; 
			this.objName = objName;
		} 
		
		public void run() { 
			try { 
				registry.unbind(objName); 
			} 
			catch (RemoteException | NotBoundException e) { 
				e.printStackTrace(); 
			} 
		}; 
	}

}
