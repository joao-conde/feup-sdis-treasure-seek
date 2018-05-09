package sdis.communications;

import android.content.Context;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Scanner;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import sdis.util.ParseMessageException;

public class ConnectionHelper {

    private static final String[] ENC_PROTOCOLS = new String[] {"TLSv1.2"};
    private static final int TIME_OUT = 5000;

    private static class Connection {

        protected int port;
        protected String hostAddress;

        public Connection(String hostAddress, int port)  {
            this.port = port;
            this.hostAddress = hostAddress;
        }


        protected Socket buildSocket() throws IOException {
            return new Socket();
        }

        public ServerMessage sendMessage(String message) throws IOException, JSONException, ParseMessageException {

            Socket socket = buildSocket();

            SocketAddress sockAddress = new InetSocketAddress(hostAddress, port);
            socket.connect(sockAddress, TIME_OUT);


            PrintWriter pw = new PrintWriter(socket.getOutputStream(), true);
            Scanner scanner = new Scanner(new BufferedReader(new InputStreamReader(socket.getInputStream())));
            ServerMessage response = null;

            pw.println(message);

            socket.setSoTimeout(TIME_OUT);

            if(scanner.hasNextLine())
                response = ServerMessage.parseServerMessage(scanner.nextLine());

            return response;

        }


    }

    private static class SSLConnection extends Connection {

        public static String[] ENC_PROTOCOLS = new String[] {"TLSv1.2"};
        private SSLContext sslContext;

        public SSLConnection(String hostAddress, int port, SSLContext sslContext) throws IOException {
            super(hostAddress,port);
            this.sslContext = sslContext;

        }

        @Override
        protected Socket buildSocket() throws IOException {

            SSLSocketFactory factory = sslContext.getSocketFactory();

            //SSLSocket socket = (SSLSocket) factory.createSocket(InetAddress.getByName(hostAddress),port);

            SSLSocket socket = (SSLSocket) factory.createSocket();

            socket.setEnabledProtocols(ENC_PROTOCOLS);
            socket.setEnabledCipherSuites(socket.getSupportedCipherSuites());

            return socket;

        }

    }

    private SSLContext sslContext;
    private Context context;

    public ConnectionHelper(Context context) {

        this.context = context;
        this.sslContext = createSSLContextWithCustomCertificate();

    }

    private SSLContext createSSLContextWithCustomCertificate() {

        Certificate ca = this.getCertificateFromFile();
        KeyStore keyStore = this.createKeyStoreWithCA(ca);

        // Create a TrustManager that trusts the CAs in our KeyStore
        // Create an SSLContext that uses our TrustManager
        String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
        TrustManagerFactory tmf;

        SSLContext context = null;

        try {
            tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
            tmf.init(keyStore);
            context = SSLContext.getInstance(ENC_PROTOCOLS[0]);
            context.init(null, tmf.getTrustManagers(), null);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }

        return context;

    }

    private Certificate getCertificateFromFile() {

        Certificate ca = null;

        try {

            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            InputStream caInput =  context.getResources().openRawResource(context.getResources().getIdentifier("treasureseek", "raw",context.getPackageName()));
            ca = cf.generateCertificate(caInput);
            System.out.println("ca=" + ((X509Certificate) ca).getSubjectDN());
            caInput.close();
        }


        catch (CertificateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return ca;

    }

    private KeyStore createKeyStoreWithCA(Certificate ca) {

        String keyStoreType = KeyStore.getDefaultType();
        KeyStore keyStore = null;

        try {
            keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(null, null);
            keyStore.setCertificateEntry("ca", ca);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        }

        return keyStore;
    }

    public ServerMessage sendMessage(String message, String address, int port) throws JSONException, ParseMessageException, IOException {

        return new Connection(address,port).sendMessage(message);

    }

    public ServerMessage sendMessageOverSSL(String message, String address, int port) throws IOException, JSONException, ParseMessageException {

        return new SSLConnection(address,port,sslContext).sendMessage(message);

    }



}
