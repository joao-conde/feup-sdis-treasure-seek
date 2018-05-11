package main;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.AccessException;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ExportException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.sql.Statement;
import java.util.Scanner;
import java.util.concurrent.Callable;

import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;

import model.Treasure;
import model.User;
import util.DBManagerAlreadyExistsException;
import util.Utils;

public class DBServer extends UnicastRemoteObject implements DBOperations {

	private static final long serialVersionUID = 1L;
	public static String[] ENC_PROTOCOLS = new String[] { "TLSv1.2" };
	public static String[] ENC_CYPHER_SUITES = new String[] { "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256" };

	private static final String DB_PATH = "../db/";
	
    private static final String RMI_PREFIX = "db_";

    private String DBNAME;
    private String DBURL;
    public int dbNo;
    
    private Registry registry;
    private Connection connection;
    
    private DBManagerOperations dbManagerRemoteObj;
   
    
    protected DBServer(String dbManagerAddress) throws Exception {
		super(0, new SslRMIClientSocketFactory(), new SslRMIServerSocketFactory(null, ENC_PROTOCOLS, false));
		
		registry = LocateRegistry.getRegistry(
				dbManagerAddress, Registry.REGISTRY_PORT,
				new SslRMIClientSocketFactory());
		System.out.println("dbManager registry loaded");
		
		dbManagerRemoteObj = (DBManagerOperations) registry.lookup("db_manager");

		dbNo = dbManagerRemoteObj.newDBServer(this);
		
		System.out.println("dbServer created, No: " + dbNo);

		createConnection();
	}

	public static void main(String[] args) throws Exception {
		
		Utils.setSecurityProperties();  
		
		String dbManagerAddress = args[0];
		DBServer dbServer = new DBServer(dbManagerAddress);	
	
		Runtime.getRuntime().addShutdownHook(new Thread(new DBServer.CloseDBServer(dbServer)));
	}	
	
	private void createConnection() throws SQLException, FileNotFoundException {
		DBNAME = "treasureSeekDB" + dbNo + ".db";
		DBURL = "jdbc:sqlite:../db/" + DBNAME;

		boolean dbFileExists = new File(DB_PATH + DBNAME).exists();

		connection = DriverManager.getConnection(DBURL);

		if (!dbFileExists) {
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

	static class CloseDBServer implements Runnable{
		DBServer dbServer;
	
		public CloseDBServer(DBServer dbServer) {
			this.dbServer = dbServer;
		}

		public void run() {
			try {
				dbServer.dbManagerRemoteObj.removeDBServer(dbServer.dbNo);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		};
	}

	@Override
	public User insertUser(boolean appServerRequest, long id, String email, String token, String name)
			throws RemoteException {

		try {

			PreparedStatement stmt = connection
					.prepareStatement("INSERT INTO user (id, email, token, name) VALUES (?, ?, ?, ?)");

			stmt.setLong(1, id);
			stmt.setString(2, email);
			stmt.setString(3, token);
			stmt.setString(4, name);
			stmt.executeUpdate();

			System.out.println("User inserted with success.");

			User user = new User();
			user.setValue("id", id);
			user.setValue("email", email);
			user.setValue("token", token);
			user.setValue("name", name);
			user.setValue("admin", false);

			if (appServerRequest) {

				Thread replicateChangeThread = new Thread() {
					public void run() {

						try {
							String[] dbServers;
							dbServers = registry.list();
							for (String db : dbServers) {
								if (db.equals(RMI_PREFIX + dbNo))
									continue;

								((DBOperations) registry.lookup(db)).insertUser(false, id, email, token, name);

							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				};

				replicateChangeThread.start();
			}

			return user;

		} catch (SQLException e) {
			System.out.println(e.getMessage());
			return null;
		}

	}

	@Override
	public User getUser(boolean appServerRequest, long id) throws RemoteException, SQLException {

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
	public boolean updateUser(boolean appServerRequest, long id, String token) throws RemoteException, SQLException {

		try {

			PreparedStatement stmt = connection.prepareStatement("UPDATE user SET token = ? WHERE id = ?");

			stmt.setString(1, token);
			stmt.setLong(2, id);
			stmt.executeUpdate();

			if (appServerRequest) {
				
				Thread replicateChangeThread = new Thread() {
					public void run() {

						try {
							String[] dbServers;
							dbServers = registry.list();
							for (String db : dbServers) {
								if (db.equals(RMI_PREFIX + dbNo))
									continue;

								((DBOperations) registry.lookup(db)).updateUser(false, id, token);
								System.out.println("updateUser called for DB" + db);

							}
						} catch (RemoteException | SQLException | NotBoundException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				};
				
				replicateChangeThread.start();
			}

			System.out.println("User updated with success on DB" + this.dbNo);
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
	public ArrayList<Treasure> getAllTreasuresWithFoundInfo() throws RemoteException, SQLException {
		
		PreparedStatement stmt = connection.prepareStatement(
			"SELECT * FROM (" +
				"SELECT *, 0 as found" + 
				"FROM treasure" + 
				"WHERE (1, treasure.id)" + 
				"IN (select * from user_treasure)" + 
				
				"UNION" + 
				
				"SELECT *, 1 as found" + 
				"FROM treasure" + 
				"WHERE (1, treasure.id)" + 
				"NOT IN (select * from user_treasure)" +
			");"
        );
		
		ResultSet result = stmt.executeQuery();
		ArrayList<Treasure> treasures = new ArrayList<>();
		
		while(result.next()) {
			
			Treasure treasure = new Treasure();
			treasure.setValue("id", result.getInt(1));
			treasure.setValue("latitude", result.getDouble(2));
			treasure.setValue("longitude", result.getDouble(3));
			treasure.setValue("description", result.getString(4));
			treasure.setValue("found", result.getInt(5));
			treasures.add(treasure);
			
		}
		
					
		return treasures;
		
		
	}

}
