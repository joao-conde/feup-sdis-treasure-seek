package main;

import java.rmi.RemoteException;

public interface DBManagerOperations extends DBOperations {

	int newDBServer(DBOperations dbServer) throws RemoteException;
	boolean removeDBServer(int dbNo) throws RemoteException;

}
