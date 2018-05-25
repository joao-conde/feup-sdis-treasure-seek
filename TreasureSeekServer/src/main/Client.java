package main;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Scanner;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.json.JSONException;
import org.json.JSONObject;

import communications.Message;
import model.Model;
import model.User;

public class Client {

	private static int serverPort = 6789;
	private static String userID = "2065426510151728";
	
    private static final String[] ENC_PROTOCOLS = new String[] {"TLSv1.2"};

	private static String host;
	private String token;
	private static int appServerPort;

	public static void main(String[] args) throws UnknownHostException, IOException, InterruptedException {
		new Client(args[0], Integer.parseInt(args[1]), Integer.parseInt(args[2]), args[3]);
		System.out.println("---CLIENT FINISHED---");
	}

	public Client(String host, int port, int loginAttempts, String token)
			throws UnknownHostException, IOException, InterruptedException {

		this.host = host;
		this.appServerPort = port;
		this.token = token;

		for (int i = 0; i < loginAttempts; i++) {

			JSONObject json = new JSONObject();
			try {
				json.put("token", token);
			} catch (JSONException e) {
				e.printStackTrace();
			}

			String message = "LOGIN users " + json.toString();
			
            SSLSocket socket = (SSLSocket) SSLSocketFactory.getDefault().createSocket();

            socket.setEnabledProtocols(ENC_PROTOCOLS);
            socket.setEnabledCipherSuites(socket.getSupportedCipherSuites());

			SocketAddress sockAddress = new InetSocketAddress(host, port);
			socket.connect(sockAddress, 2000);
			
			/*Socket socket = new Socket();
			SocketAddress sockAddress = new InetSocketAddress(host, port);
			socket.connect(sockAddress, 2000);*/

			PrintWriter pw = new PrintWriter(socket.getOutputStream(), true);
			Scanner scanner = new Scanner(new BufferedReader(new InputStreamReader(socket.getInputStream())));

			pw.println(message);
			
			pw.close();
			scanner.close();
		}
	}
	


}
