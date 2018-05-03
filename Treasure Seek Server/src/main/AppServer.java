package main;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.sql.SQLException;
import java.util.Arrays;

public class AppServer{

    private static String[] dbServerIPs;

    public static void main(String[] args) throws RemoteException, NotBoundException, SQLException{
    	
    	int registryPort = 1099;
    	if (args.length > 0)
    		registryPort = Integer.parseInt(args[0]);
    	
    	//TODO: get an sslContext with a keystore
		if(args.length > 1)
			dbServerIPs = Arrays.copyOfRange(args, 1, args.length);

        
        Registry registry = LocateRegistry.getRegistry(registryPort);
        DBOperations dbOperations = (DBOperations) registry.lookup("db_operations");
        
        dbOperations.insertUser("leonardomgt", "leo@exemplo.com", "qwertyuioplkjhgfdsa");
        
    }

    /*
    ServerSocketFactory ssf = ServerSocketFactory.getDefault();
    ServerSocket serverSocket = ssf.createServerSocket(12345);
    
    Socket socket = serverSocket.accept();

    SSLSocketFactory sslSf = sslContext.getSocketFactory();
    
    
    SSLSocket sslSocket = (SSLSocket) sslSf.createSocket(socket, null, socket.getPort(), false);
    sslSocket.setUseClientMode(false);
    */
    
    //Use as a normal socket after



}