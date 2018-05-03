package main;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.sql.SQLException;

public interface DBOperations extends Remote {
	void insertUser(String username, String email, String token) throws RemoteException, SQLException;
}
