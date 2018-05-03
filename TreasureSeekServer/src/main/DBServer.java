package main;

import java.net.InetAddress;
import java.net.UnknownHostException;
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

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;

public class DBServer extends UnicastRemoteObject implements DBOperations{

	public static String[] ENC_PROTOCOLS = new String[] {"TLSv1.2"};
	public static String[] ENC_CYPHER_SUITES = new String[] {"TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256"};
	
    private static final int REGISTRY_PORT = 1099;
    private static final String DBNAME = "treasureSeekDB.db";
    private static final String DBURL = "jdbc:sqlite:db/" + DBNAME;

    
    protected DBServer() throws Exception {
    	super(0, new SslRMIClientSocketFactory(), new SslRMIServerSocketFactory(null, ENC_PROTOCOLS, false));
    }
    
	public static void main(String[] args) throws Exception {
		
		setSecurityProperties();        
		
		Registry registry;
		
		try{
			registry = LocateRegistry.createRegistry(REGISTRY_PORT,
		            new SslRMIClientSocketFactory(),
		            new SslRMIServerSocketFactory(null, ENC_PROTOCOLS, false));
            System.out.println("registry created.");
		}
		catch(ExportException e){
			registry = LocateRegistry.getRegistry(
	                InetAddress.getLocalHost().getHostName(), REGISTRY_PORT,
	                new SslRMIClientSocketFactory());
            System.out.println("registry loaded.");
		}
		
		DBServer dbServer = new DBServer();
//		DBOperations dbOps = (DBOperations) UnicastRemoteObject.exportObject(dbServer, 0);

		int dbNo = 1;
		while(true) {
			try{
				registry.bind("db_" + dbNo, dbServer); 
	            System.out.println("obj bound: db_" + dbNo);
				break;
			}
			catch(AlreadyBoundException e) {
	            System.out.println("obj already bound: db_" + dbNo);
				dbNo++;
			}
			catch(RemoteException e) {
	            System.out.println("Remote exception");
				break;
			}
		}
		
		Runtime.getRuntime().addShutdownHook(new Thread(new DBServer.CloseDBServer(dbNo)));
	}	
	
	 private static void setSecurityProperties() {
		 String password = "123456";
		 System.setProperty("javax.net.ssl.keyStore", "security/keys/keystore");
		 System.setProperty("javax.net.ssl.keyStorePassword", password);
		 
		 System.setProperty("javax.net.ssl.trustStore", "security/certificates/truststore");
		 System.setProperty("javax.net.ssl.trustStorePassword", password);
		
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
				Registry registry = LocateRegistry.getRegistry(
		                InetAddress.getLocalHost().getHostName(), REGISTRY_PORT,
		                new SslRMIClientSocketFactory());
				registry.unbind("db_" + dbNo);
			} catch (RemoteException | NotBoundException | UnknownHostException e) {
				e.printStackTrace();
			}
		};
	}

}
