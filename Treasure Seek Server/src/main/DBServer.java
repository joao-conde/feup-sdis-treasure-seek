package main;

import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DBServer implements DBOperations{

	private static DBServer instance = null;
    
    private static String DBNAME = "treasureSeekDB.db";
    private static String DBURL = "jdbc:sqlite:db/" + DBNAME;

    public static DBServer getInstance() {
    	if (instance == null)
    		instance = new DBServer();
	    return instance;
	}
    
	public static void main(String[] args) throws RemoteException, AlreadyBoundException, SQLException {
		
		instance = new DBServer();
		DBOperations dbOps = (DBOperations) UnicastRemoteObject.exportObject(instance, 0);
        
		int registryPort = args.length > 0 ? Integer.parseInt(args[0]) : 1099;
		instance = new DBServer();


		Registry registry = LocateRegistry.createRegistry(registryPort);
		registry.bind("operations", dbOps); 
		
		
		
	}
	
	
	
	 /**
     * Connect to a sample database
     */
	@Override
	public void insertUser(String username, String email, String token) {
    	Connection connection = null;
        try {
            // create a connection to the database
            connection = DriverManager.getConnection(DBURL);

            System.out.println("Connection to SQLite has been established.");

            PreparedStatement stmt = connection.prepareStatement(
            	"INSERT INTO user VALUES (?, ?, ?)"
            );

            stmt.setString(1, username);
            stmt.setString(2, email);
            stmt.setString(3, token);
            stmt.executeUpdate();
            
            System.out.println("User inserted with success.");

            
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
	

}
