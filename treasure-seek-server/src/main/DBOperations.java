package main;

import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.ArrayList;

import model.Treasure;
import model.User;
import util.Utils.Pair;

public interface DBOperations extends Remote {

	// User

	User insertUser(long id, String email, String token, String name, String address, ArrayList<String> dbServerHostAddresses) throws RemoteException, SQLException;
	User getUser(long id) throws RemoteException, SQLException;
	boolean updateUser(long id, String token, String address, ArrayList<String> dbServerHostAddresses) throws RemoteException, SQLException;
	
	// Treasures
	boolean insertTreasure(Double latitude, Double longitude, long userCreatorId, String description, String challenge, String challengeSolution, ArrayList<String> dbServerHostAddresses) throws RemoteException, SQLException;	
	Treasure getTreasure(int treasureId) throws RemoteException, SQLException;
	ArrayList<Treasure> getAllTreasures() throws RemoteException, SQLException;
	Pair<ArrayList<Treasure>,ArrayList<Treasure>> getAllTreasuresWithFoundInfo(long userId) throws RemoteException, SQLException;
	
	// Found Treasure
	boolean insertFoundTreasure(int treasureId, long userId, ArrayList<String> dbServerHostAddresses) throws RemoteException, SQLException;
	
	// Other functions
	ArrayList<String> getSubscribedUsersAddress() throws RemoteException, SQLException;
	ArrayList<Pair<User, Integer>> getRanking() throws RemoteException, SQLException;

	// System
	byte[] recoverDB() throws RemoteException, IOException;
	
}
