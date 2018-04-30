package main;

import java.rmi.Remote;

public interface DBOperations extends Remote {
	String insertUser(String username, String token);
}
