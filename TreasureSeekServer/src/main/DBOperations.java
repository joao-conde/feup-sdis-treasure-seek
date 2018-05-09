package main;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.sql.SQLException;

import model.User;

public interface DBOperations extends Remote {
	User insertUser(long id, String email, String token, String name) throws RemoteException, SQLException;
	User getUser(long id) throws RemoteException, SQLException;
	boolean updateUser(long id, String token) throws RemoteException, SQLException;
}
