package main;

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
import java.sql.SQLException;

public class DBServer implements DBOperations{

	private static DBServer instance = null;

    private static final int REGISTRY_PORT = 1100;
    private static final String DBNAME = "treasureSeekDB.db";
    private static final String DBURL = "jdbc:sqlite:db/" + DBNAME;

    public static DBServer getInstance() {
    	if (instance == null)
    		instance = new DBServer();
	    return instance;
	}



    
	public static void main(String[] args) throws RemoteException, SQLException {
		
		instance = new DBServer();
		DBOperations dbOps = (DBOperations) UnicastRemoteObject.exportObject(instance, 0);
        
		
		Registry registry;
		
		try{
			registry = LocateRegistry.createRegistry(REGISTRY_PORT);
            System.out.println("registry created.");
		}
		catch(ExportException e){
			registry = LocateRegistry.getRegistry(REGISTRY_PORT);
            System.out.println("registry loaded.");
		}
		
		int dbNo = 1;
		while(true) {
			try{
				registry.bind("db_" + dbNo, dbOps); 
	            System.out.println("obj bound: db_" + dbNo);
				break;
			}
			catch(AlreadyBoundException e) {
	            System.out.println("obj already bound: db_" + dbNo);
				dbNo++;
			}
		}
		
		Runtime.getRuntime().addShutdownHook(new Thread(new DBServer.CloseDBServer(dbNo)));
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
            	"INSERT INTO user (username, email, token) VALUES (?, ?, ?)"
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
	
	static class CloseDBServer implements Runnable{
		int dbNo;
	
		public CloseDBServer(int dbNo) {
			this.dbNo = dbNo;
		}

		public void run() {
			try {
				Registry registry = LocateRegistry.getRegistry(REGISTRY_PORT);
				registry.unbind("db_" + dbNo);
			} catch (RemoteException | NotBoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		};
	}

}
