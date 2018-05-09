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
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;

import model.User;
import util.Utils;

/**
 * @author jotac
 *
 */
public class DBServer extends UnicastRemoteObject implements DBOperations{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static String[] ENC_PROTOCOLS = new String[] {"TLSv1.2"};
	public static String[] ENC_CYPHER_SUITES = new String[] {"TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256"};
	
    private static final int REGISTRY_PORT = 1099;
    private static final String RMI_PREFIX = "db_";

    private String DBNAME;
    private String DBURL;
    public int dbNo;
    
    private Registry registry;
    private Connection connection;

    
    protected DBServer() throws Exception {
		super(0, new SslRMIClientSocketFactory(), new SslRMIServerSocketFactory(null, ENC_PROTOCOLS, false));
		
		try{
			registry = LocateRegistry.createRegistry(REGISTRY_PORT,
		            new SslRMIClientSocketFactory(),
		            new SslRMIServerSocketFactory(null, ENC_PROTOCOLS, true));
            System.out.println("registry created.");
		}
		catch(ExportException e){
			registry = LocateRegistry.getRegistry(
	                InetAddress.getLocalHost().getHostName(), REGISTRY_PORT,
	                new SslRMIClientSocketFactory());
            System.out.println("registry loaded.");
		}
		
		dbNo = 1;
		while(true) {
			try{

				registry.bind(RMI_PREFIX + dbNo, this); 
	            System.out.println("obj bound: " + RMI_PREFIX + dbNo);
				break;
			}
			catch(AlreadyBoundException e) {
	            System.out.println("obj already bound: " + RMI_PREFIX + dbNo);
				dbNo++;
			}
			catch(RemoteException e) {
	            System.out.println("Remote exception");
				break;
			}
		}
		
		DBNAME = "treasureSeekDB" + dbNo + ".db";
		DBURL = "jdbc:sqlite:../db/" + DBNAME;
		connection = DriverManager.getConnection(DBURL);
    }
    
	public static void main(String[] args) throws Exception {
		
		Utils.setSecurityProperties();       
		
		DBServer dbServer = new DBServer();		
	
		Runtime.getRuntime().addShutdownHook(new Thread(new DBServer.CloseDBServer(dbServer.dbNo)));
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
				registry.unbind(RMI_PREFIX + dbNo);
			} catch (RemoteException | NotBoundException | UnknownHostException e) {
				e.printStackTrace();
			}
		};
	}
	
	
	@Override
	public User insertUser(boolean appServerRequest, long id, String email, String token, String name) throws RemoteException {
    	
        try {

            PreparedStatement stmt = connection.prepareStatement(
            	"INSERT INTO user (id, email, token, name) VALUES (?, ?, ?, ?)"
            );

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
    			
    			
    		if(appServerRequest) {
    	        String[] dbServers = registry.list();
    	        
    	        for(String db: dbServers) {
    	        	if(db.equals(RMI_PREFIX + dbNo)) continue;
    	        	
    	        	((DBOperations) registry.lookup(db)).insertUser(false, id, email, token, name);
    	        	
    	        }
    	        
    	        System.out.println("insertUser called for other DB");
    	    }
            
            return user;

            
        } catch (SQLException | NotBoundException e) {
            System.out.println(e.getMessage());
            return null;
        }
        
    }

	@Override
	public User getUser(boolean appServerRequest, long id) throws RemoteException, SQLException {
		
		PreparedStatement stmt = connection.prepareStatement(
			"SELECT * from user WHERE id = ?"
        );
		
		stmt.setLong(1, id);
		ResultSet result = stmt.executeQuery();
		
		if(!result.next())
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

            PreparedStatement stmt = connection.prepareStatement(
            	"UPDATE user SET token = ? WHERE id = ?"
            );

            
            stmt.setString(1, token);
            stmt.setLong(2, id);
            stmt.executeUpdate();
            
            
            if(appServerRequest) {
    	        String[] dbServers = registry.list();
    	        
    	        for(String db: dbServers) {
    	        	if(db.equals(RMI_PREFIX + dbNo)) continue;
    	        	
    	        	((DBOperations) registry.lookup(db)).updateUser(false, id, token);
    	        	
    	        }
    	        
    	        System.out.println("updateUser called for other DB");
    	    }
            
            
            System.out.println("User updated with success.");
            return true;

            
        } catch (SQLException | NotBoundException e) {
            System.out.println(e.getMessage());
            return false;
        } 
		
		
	}

}
