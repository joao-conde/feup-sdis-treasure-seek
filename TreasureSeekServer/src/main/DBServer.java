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

public class DBServer extends UnicastRemoteObject implements DBOperations{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static String[] ENC_PROTOCOLS = new String[] {"TLSv1.2"};
	public static String[] ENC_CYPHER_SUITES = new String[] {"TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256"};
	
    private static final int REGISTRY_PORT = 1099;
    private static final String DBNAME = "treasureSeekDB.db";
    private static final String DBURL = "jdbc:sqlite:../db/" + DBNAME;
    
    private Connection connection = DriverManager.getConnection(DBURL);

    
    protected DBServer() throws Exception {
    		super(0, new SslRMIClientSocketFactory(), new SslRMIServerSocketFactory(null, ENC_PROTOCOLS, false));
    }
    
	public static void main(String[] args) throws Exception {
		
		Utils.setSecurityProperties();       
		
		Registry registry;
		
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
	
	@Override
	public boolean insertUser(long id, String email, String token) {
    	
        try {

            PreparedStatement stmt = connection.prepareStatement(
            	"INSERT INTO user (id, email, token) VALUES (?, ?, ?)"
            );

            stmt.setLong(1, id);
            stmt.setString(2, email);
            stmt.setString(3, token);
            stmt.executeUpdate();
            
            System.out.println("User inserted with success.");
            return true;

            
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return false;
        } 
//        finally {
//            try {
//                if (connection != null) {
//                    connection.close();
//                }
//            } catch (SQLException ex) {
//                System.out.println(ex.getMessage());
//            }
//        }
    }

	@Override
	public User getUser(long id) throws RemoteException, SQLException {
		
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
		user.setValue("admin", result.getBoolean(4));
				
		return user;
		
	}

	@Override
	public void updateUser(long id, String email, String token) throws RemoteException, SQLException {
		// TODO Auto-generated method stub
		
	}

}
