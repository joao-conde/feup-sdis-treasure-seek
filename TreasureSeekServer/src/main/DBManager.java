package main;

import java.rmi.AccessException;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ExportException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Random;

import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;

import model.Treasure;
import model.User;
import util.DBManagerAlreadyExistsException;
import util.Utils;

public class DBManager extends UnicastRemoteObject implements DBManagerOperations  {
	
	private static final long serialVersionUID = 1L;
	public static String[] ENC_PROTOCOLS = new String[] { "TLSv1.2" };
	public static String[] ENC_CYPHER_SUITES = new String[] { "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256" };
	
    private Registry registry;

//	private static final String DB_PATH = "../db/";
    private static final String RMI_PREFIX = "db_";

	protected DBManager() throws RemoteException, DBManagerAlreadyExistsException, AlreadyBoundException {
		super(0, new SslRMIClientSocketFactory(), new SslRMIServerSocketFactory(null, ENC_PROTOCOLS, false));	
		
		try {
			registry = LocateRegistry.createRegistry(Registry.REGISTRY_PORT,
    	            new SslRMIClientSocketFactory(),
    	            new SslRMIServerSocketFactory(null, ENC_PROTOCOLS, true));
        	System.out.println("registry created.");
		}
		catch (ExportException e) {
			throw new DBManagerAlreadyExistsException();
		}
		
		registry.bind("db_manager", this);
	}
	
	public static void main(String[] args) {
		Utils.setSecurityProperties();  

		try {
			new DBManager();
		} 
		catch (DBManagerAlreadyExistsException e) {
			System.out.println(e.getMessage());
			System.exit(1);
		} 
		catch (RemoteException | AlreadyBoundException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public User insertUser(boolean appServerRequest, long id, String email, String token, String name)
			throws RemoteException, SQLException {		

		User user = null;
		for (String db : registry.list()) {
			if(db.equals("db_manager")) continue;
			
			try {
				DBOperations dbOperations = (DBOperations) registry.lookup(db);
				user = dbOperations.insertUser(appServerRequest, id, email, token, name);
			} catch (NotBoundException e) {
				e.printStackTrace();
			}
		}
		return user;
	}
	@Override
	public User getUser(boolean appServerRequest, long id) throws RemoteException, SQLException {

		String db = pickRandomDB();

		try {
			DBOperations dbOperations = (DBOperations) registry.lookup(db);
			return dbOperations.getUser(appServerRequest, id);
		} catch (NotBoundException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	private String pickRandomDB() throws AccessException, RemoteException {
		// randomly selects one of the available DB's
		String[] boundNames = registry.list();
		int idx;
		do{
			idx = new Random().nextInt(boundNames.length);
		}
		while(boundNames[idx].equals("db_manager"));		
		
		return boundNames[idx];
	}

	@Override
	public boolean updateUser(boolean appServerRequest, long id, String token) throws RemoteException, SQLException {
		boolean result = true;
		
		for (String db : registry.list()) {
			if(db.equals("db_manager")) continue;
			
			try {
				DBOperations dbOperations = (DBOperations) registry.lookup(db);
				result = result && dbOperations.updateUser(appServerRequest, id, token);
			} catch (NotBoundException e) {
				e.printStackTrace();
			}
		}		
		return result;
	}
	@Override
	public ArrayList<Treasure> getAllTreasures() throws RemoteException, SQLException {


		String db = pickRandomDB();

		try {
			DBOperations dbOperations = (DBOperations) registry.lookup(db);
			return dbOperations.getAllTreasures();
		} catch (NotBoundException e) {
			e.printStackTrace();
		}
		
		return null;
		
	}
	@Override
	public ArrayList<Treasure> getAllTreasuresWithFoundInfo() throws RemoteException, SQLException {
		String db = pickRandomDB();

		try {
			DBOperations dbOperations = (DBOperations) registry.lookup(db);
			return dbOperations.getAllTreasuresWithFoundInfo();
		} catch (NotBoundException e) {
			e.printStackTrace();
		}
		
		return null;

	}
	@Override
	public int newDBServer(DBOperations dbServer) throws RemoteException {
		System.out.println("New DB Server");

		int dbNo = 1;
		while (true) {
			try {
				registry.bind(RMI_PREFIX + dbNo, dbServer);
				System.out.println("obj bound: " + RMI_PREFIX + dbNo);
				break;
			} catch (AlreadyBoundException e) {
				System.out.println("obj already bound: " + RMI_PREFIX + dbNo);
				dbNo++;
			}
			catch(RemoteException e) {
				e.printStackTrace();
			}
		}
		return dbNo;
	}

	@Override
	public boolean removeDBServer(int dbNo) throws RemoteException {

		try {
			registry.unbind(RMI_PREFIX + dbNo);
		} catch (NotBoundException e) {
			return false;
		}
		return true;
	}

	@Override
	public boolean validateTreasure(int treasureId, String answer) throws RemoteException, SQLException {
		boolean result = true;
		
		for (String db : registry.list()) {
			if(db.equals("db_manager")) continue;
			
			try {
				DBOperations dbOperations = (DBOperations) registry.lookup(db);
				result = result && dbOperations.validateTreasure(treasureId, answer);
			} catch (NotBoundException e) {
				e.printStackTrace();
			}
		}		
		return result;
	}

	@Override
	public boolean insertFoundTreasure(int treasureId, long userId) throws RemoteException, SQLException {
		boolean result = true;
		
		for (String db : registry.list()) {
			if(db.equals("db_manager")) continue;
			
			try {
				DBOperations dbOperations = (DBOperations) registry.lookup(db);
				result = result && dbOperations.insertFoundTreasure(treasureId, userId);
			} catch (NotBoundException e) {
				e.printStackTrace();
			}
		}		
		return result;
	}

}
