package main;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import com.sun.net.ssl.SSLContext;

public class AppServer{

    private String[] dbServerIPs;
    private ServerSocket serverSocket;

    public static void main(String[] args){
        AppServer server = new AppServer(args);
    }

    public AppServer(String[] dbServerIPs){
        System.out.println("---App Server---");

        //TODO: get an sslContext with a keystore
        this.dbServerIPs = dbServerIPs;
        
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


}