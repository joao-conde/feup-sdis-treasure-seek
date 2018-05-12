package main;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.ArrayList;

import model.Treasure;
import model.User;
import util.Utils.Pair;

public interface DBOperations extends Remote {

	User insertUser(boolean appServerRequest, long id, String email, String token, String name) throws RemoteException, SQLException;
	User getUser(boolean appServerRequest, long id) throws RemoteException, SQLException;
	boolean updateUser(boolean appServerRequest, long id, String token) throws RemoteException, SQLException;
	ArrayList<Treasure> getAllTreasures() throws RemoteException, SQLException;
	Pair<ArrayList<Treasure>,ArrayList<Treasure>> getAllTreasuresWithFoundInfo(long userId) throws RemoteException, SQLException;
	boolean validateTreasure(int treasureId, String answer) throws RemoteException, SQLException;
	boolean insertFoundTreasure(int treasureId, long userId) throws RemoteException, SQLException;
}
