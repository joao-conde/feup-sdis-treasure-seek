package main;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.ArrayList;

import model.Treasure;
import model.User;
import util.Utils.Pair;

public interface DBOperations extends Remote {

	User insertUser(long id, String email, String token, String name, String address, ArrayList<String> dbServerHostAddresses) throws RemoteException, SQLException;
	User getUser(long id) throws RemoteException, SQLException;
	boolean updateUser(long id, String token, String address, ArrayList<String> dbServerHostAddresses) throws RemoteException, SQLException;
	ArrayList<Treasure> getAllTreasures() throws RemoteException, SQLException;
	Pair<ArrayList<Treasure>,ArrayList<Treasure>> getAllTreasuresWithFoundInfo(long userId) throws RemoteException, SQLException;
	Treasure getTreasure(int treasureId) throws RemoteException, SQLException;
	
	boolean insertFoundTreasure(int treasureId, long userId, ArrayList<String> dbServerHostAddresses) throws RemoteException, SQLException;
	
	boolean insertTreasure(Double latitude, Double longitude, long userCreatorId, String description, String challenge, String challengeSolution, ArrayList<String> dbServerHostAddresses) throws RemoteException, SQLException;	
	//	void newDBServer(String dbServerAddress) throws RemoteException;
//	boolean removeDBServer(String address) throws RemoteException;
}
