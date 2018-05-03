package sdis.treasureseek;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.Profile;

import com.facebook.ProfileTracker;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class MainActivity extends AppCompatActivity {

    public static String[] ENC_PROTOCOLS = new String[] {"TLSv1.2"};
    public static String[] ENC_CYPHER_SUITES = new String[] {"TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256"};
    public static int SERVER_PORT = 2000;

    CallbackManager facebookCallbackManager = CallbackManager.Factory.create();
    LoginButton loginButton;
    TextView usernameTextView;
    AccessToken facebookAccessToken;
    ProfileListener profileListener;

    SSLContext sslContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        System.setProperty("java.net.preferIPv4Stack" , "true");
        setContentView(R.layout.activity_main);


        this.sslContext = this.createSSLContextWithCustomCertificate();


        loginButton = findViewById(R.id.login_button);
        loginButton.setReadPermissions(this.getResources().getStringArray(R.array.facebook_permissions));
        loginButton.registerCallback(facebookCallbackManager, new TreasureSeekFacebookCallback());

        usernameTextView = this.findViewById(R.id.user_name);
        profileListener = new ProfileListener();

        this.facebookAccessToken = AccessToken.getCurrentAccessToken();

        if(facebookAccessToken != null) {

            if(Profile.getCurrentProfile() != null) {
                usernameTextView.setText(Profile.getCurrentProfile().getName());
                new LoginToTreasureSeek().execute();
            }
        }

        else {

            this.usernameTextView.setText(R.string.default_username);

        }

        
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        facebookCallbackManager.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }

    private class TreasureSeekFacebookCallback implements  FacebookCallback<LoginResult> {


        @Override
        public void onSuccess(LoginResult loginResult) {

            System.out.println(loginResult);
            new LoginToTreasureSeek().execute();

        }

        @Override
        public void onCancel() {

            System.out.println("Canceled");


        }

        @Override
        public void onError(FacebookException error) {

            System.out.println(error);

        }
    }


    private class ProfileListener extends ProfileTracker {

        @Override
        protected void onCurrentProfileChanged(Profile oldProfile, Profile currentProfile) {

            if(currentProfile == null)
                usernameTextView.setText(R.string.default_username);
            else
                usernameTextView.setText(currentProfile.getName());
        }
    }

    class LoginToTreasureSeek extends AsyncTask<Void,Void,Void> {

        @Override
        protected Void doInBackground(Void... voids) {

            try  {

                //SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();

                SSLSocketFactory factory = sslContext.getSocketFactory();
                SSLSocket socket = (SSLSocket) factory.createSocket(InetAddress.getByName("10.0.2.2"),SERVER_PORT);

                socket.setEnabledProtocols(ENC_PROTOCOLS);
                socket.setEnabledCipherSuites(socket.getSupportedCipherSuites());


                PrintWriter pw = new PrintWriter(socket.getOutputStream());
                pw.println("fb token: " + facebookAccessToken.getToken());
                pw.close();


            }

            catch (IOException e) {
                System.out.println(e.getLocalizedMessage());
            }


            return null;
        }
    }

    private void trustStoreAux() {

        try {

            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init((KeyStore) null);

            X509TrustManager xtm = (X509TrustManager) tmf.getTrustManagers()[0];

            for (X509Certificate cert : xtm.getAcceptedIssuers()) {
                String certStr = "S:" + cert.getSubjectDN().getName() + "\nI:"
                        + cert.getIssuerDN().getName();

                Log.d("cert", certStr);

            }



        } catch (NoSuchAlgorithmException | KeyStoreException e) {
            e.printStackTrace();
        }

    }


    private SSLContext createSSLContextWithCustomCertificate() {

        Certificate ca = this.getCertificateFromFile();
        KeyStore keyStore = this.createKeyStoreWithCA(ca);

        // Create a TrustManager that trusts the CAs in our KeyStore
        // Create an SSLContext that uses our TrustManager
        String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
        TrustManagerFactory tmf = null;

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
            InputStream caInput = getResources().openRawResource(getResources().getIdentifier("treasureseek", "raw", getPackageName()));
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


}
