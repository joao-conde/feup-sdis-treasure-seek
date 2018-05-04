package main;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.sql.SQLException;

import model.User;

public interface DBOperations extends Remote {
	boolean insertUser(long id, String email, String token) throws RemoteException, SQLException;
	User getUser(long id) throws RemoteException, SQLException;
	void updateUser(long id, String email, String token) throws RemoteException, SQLException;
}
