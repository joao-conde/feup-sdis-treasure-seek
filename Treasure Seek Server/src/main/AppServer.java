package main;


public class AppServer{

    private String[] dbServerIPs;

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