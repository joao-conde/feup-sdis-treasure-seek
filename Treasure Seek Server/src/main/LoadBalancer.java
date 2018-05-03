package main;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Timer;
import java.util.TimerTask;

public class LoadBalancer{

    private final static String multicastAddress = "224.0.0.133";
    private final static int multicastPort = 1025;

    private final static int broadcastInitialDelay = 0; //in milliseconds
    private final static int broadcastRate = 2000; //in milliseconds

    
    private String broadcastMsg;
    private DatagramPacket broadcastPacket;
    private MulticastSocket multicastSocket;
    

    public static void main(String[] args){
        LoadBalancer lb = new LoadBalancer();
    }

    public LoadBalancer(){

        this.broadcastMsg = this.multicastAddress + " " + this.multicastPort;

        try{
            InetAddress inetAdd = InetAddress.getByName(this.multicastAddress);
            this.multicastSocket = new MulticastSocket(this.multicastPort);
            this.multicastSocket.joinGroup(inetAdd);
            this.broadcastPacket = new DatagramPacket(this.broadcastMsg.getBytes(), this.broadcastMsg.getBytes().length, inetAdd, this.multicastPort);
        }
        catch(IOException e){
            e.printStackTrace();
        }
        
        this.broadcastIP();
    }


    public void broadcastIP(){

        Timer timer = new Timer();

        timer.scheduleAtFixedRate(new TimerTask(){
        
            @Override
            public void run() {

                   
                try {
                    System.out.println("\n---MESSAGE---\n" + LoadBalancer.this.broadcastMsg);
                    LoadBalancer.this.multicastSocket.send(LoadBalancer.this.broadcastPacket);
                } 
                catch (IOException e) {
                    System.err.println("Failure broadcasting message");
                    e.printStackTrace();
                }
                    
                
            }
        }, LoadBalancer.broadcastInitialDelay, LoadBalancer.broadcastRate);
    }


}