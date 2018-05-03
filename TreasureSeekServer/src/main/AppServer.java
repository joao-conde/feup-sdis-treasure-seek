package main;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.sql.SQLException;

import javax.rmi.ssl.SslRMIClientSocketFactory;

public class AppServer{

    private static final int REGISTRY_PORT = 1099;

    private static String[] dbServerIPs;

    public static void main(String[] args) throws RemoteException, NotBoundException, SQLException, UnknownHostException{
    	
    	setSecurityProperties();
    	
		dbServerIPs = args;

        Registry registry = LocateRegistry.getRegistry(
                InetAddress.getLocalHost().getHostName(), REGISTRY_PORT,
                new SslRMIClientSocketFactory());
        DBOperations dbOperations = (DBOperations) registry.lookup("db_2");
        
        dbOperations.insertUser("leonardomgt", "leo@exemplo.com", "qwertyuioplkjhgfdsa");
        
    }
    
    private static void setSecurityProperties() {
		 String password = "123456";
		 System.setProperty("javax.net.ssl.keyStore", "security/keys/keystore");
		 System.setProperty("javax.net.ssl.keyStorePassword", password);
		 
		 System.setProperty("javax.net.ssl.trustStore", "security/certificates/truststore");
		 System.setProperty("javax.net.ssl.trustStorePassword", password);
		
	}
}