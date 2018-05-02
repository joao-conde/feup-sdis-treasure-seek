package main;

import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DBServer implements DBOperations{

	private static DBServer instance = null;
    private static Connection connection = null;
    private static String DBNAME = "treasureSeekDB.db";

    public static DBServer getInstance() {
    	if (instance == null)
    		instance = new DBServer();
	    return instance;
	}
    
	public static void main(String[] args) throws RemoteException, AlreadyBoundException {
		
		instance = new DBServer();
		DBOperations dbOps = (DBOperations) UnicastRemoteObject.exportObject(instance, 0);
        
		int registryPort = args.length > 0 ? Integer.parseInt(args[0]) : 1099;
		instance = new DBServer();

		Registry registry = LocateRegistry.createRegistry(registryPort);
		registry.bind("operations", dbOps); 
		
		connect();

	}
	
	 /**
     * Connect to a sample database
     */
    public static void connect() {
        try {
            String url = "jdbc:sqlite:db/" + DBNAME;
            // create a connection to the database
            connection = DriverManager.getConnection(url);
            
            System.out.println("Connection to SQLite has been established.");
            
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException ex) {
                System.out.println(ex.getMessage());
            }
        }
    }
    

	@Override
	public void insertUser(String username, String email, String token) throws SQLException {

		Statement stmt = connection.createStatement();
        String sql= "INSERT INTO user (username,email,token) " +
        			"VALUES (" + username + ", " + email + ", " + token + ");"; 
        stmt.executeUpdate(sql);

        
	}
	

}
