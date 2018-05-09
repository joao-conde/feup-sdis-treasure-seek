package sdis.treasureseek;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;

import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.IOException;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import sdis.communications.ServerMessage;
import sdis.controller.Controller;
import sdis.util.NoAvailableServer;
import sdis.util.ParseMessageException;

public class MainActivity extends AppCompatActivity {

    CallbackManager facebookCallbackManager = CallbackManager.Factory.create();

    Button loginButton;
    TextView usernameTextView;
    ProgressBar progressBar;
    TextView ipTextView;

    LoginManager fbLoginManager;
    AccessToken facebookAccessToken;

    Controller controller;

    String appServerAddress;
    int appServerPort;

    private static final Pattern ipPattern = Pattern.compile("^([0-9]{1,3}\\.){3}[0-9]{1,3}$");;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        controller = new Controller(getApplicationContext());

        fbLoginManager = LoginManager.getInstance();
        fbLoginManager.registerCallback(facebookCallbackManager, new TreasureSeekFacebookCallback());

        loginButton = findViewById(R.id.login_button);
        loginButton.setOnClickListener(new LoginListener());

        progressBar = findViewById(R.id.loginProgressBar);
        progressBar.setVisibility(View.GONE);

        usernameTextView = this.findViewById(R.id.user_name);

        ipTextView = findViewById(R.id.textIp);
        ipTextView.setOnKeyListener(new IpTextViewListener());

        this.facebookAccessToken = AccessToken.getCurrentAccessToken();


        toggleButtons();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        facebookCallbackManager.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void toggleButtons() {


        if(controller.isLogged()) {

            loginButton.setText(getString(R.string.logout));
            usernameTextView.setText((String) controller.getLoggedUser().getValue("name"));
        }
        else {

            loginButton.setText(getString(R.string.login));
            loginButton.setEnabled(checkIpAddress(ipTextView.getText()));
            usernameTextView.setText(getString(R.string.default_username));


        }

    }

    private class LoginToTreasureSeek extends AsyncTask<Void,Void,ServerMessage> {


        @Override
        protected ServerMessage doInBackground(Void... voids) {

            ServerMessage reply = null;

            try  {
                reply = controller.loginToTreasureSeek(appServerAddress, appServerPort);
            }

            catch (IOException | ParseMessageException | JSONException e) {
                System.out.println(e.getLocalizedMessage());
            }

            return reply;
        }

        @Override
        protected void onPostExecute(ServerMessage reply) {

            progressBar.setVisibility(View.GONE);

            if(reply != null && reply.getStatus() == ServerMessage.ReplyMessageStatus.OK) {

                try {
                    JSONObject user = (JSONObject) reply.getBody().get(0);
                    controller.saveSession(user);


                } catch (JSONException e) {
                    showConnectionError();
                    return;
                }


            }

            else {
                showConnectionError();
                fbLoginManager.logOut();
            }


            toggleButtons();

        }
    }

    private class LogoutFromTreasureSeekTask extends AsyncTask<Void,Void,ServerMessage> {


        @Override
        protected ServerMessage doInBackground(Void... voids) {

            ServerMessage reply = null;

            try  {
                reply = controller.logoutFromTreasureSeek(appServerAddress, appServerPort);
            }

            catch (IOException | ParseMessageException | JSONException e) {
                System.out.println(e.getLocalizedMessage());
            }

            return reply;
        }

        @Override
        protected void onPostExecute(ServerMessage reply) {

            progressBar.setVisibility(View.GONE);

            if(reply != null && reply.getStatus() == ServerMessage.ReplyMessageStatus.OK) {
                controller.deleteSession();
                fbLoginManager.logOut();
            }


            else
                showConnectionError();


            toggleButtons();

        }

    }

    private class LoginListener implements View.OnClickListener {


        @Override
        public void onClick(View v) {

            new RequestAvailableServerTask().execute();

        }
    }

    private class TreasureSeekFacebookCallback implements  FacebookCallback<LoginResult> {


        @Override
        public void onSuccess(LoginResult loginResult) {

            facebookAccessToken = loginResult.getAccessToken();
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

    private class RequestAvailableServerTask extends AsyncTask<Void,Void,Pair<String,Integer>> {

        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected Pair<String, Integer> doInBackground(Void... voids) {

            Pair<String,Integer> server = null;

            try {
                server = controller.getAvailableServer(String.valueOf(ipTextView.getText()));


            } catch (IOException | JSONException | ParseMessageException | NoAvailableServer e) {
                e.printStackTrace();
            }

            return server;

        }

        @Override
        protected void onPostExecute(Pair<String, Integer> server) {

            if(server == null) {
                showConnectionError();
                progressBar.setVisibility(View.GONE);
                return;

            }

            appServerAddress = server.first;
            appServerPort = server.second;

            if(!controller.isLogged())
                fbLoginManager.logInWithReadPermissions(MainActivity.this, Arrays.asList(getResources().getStringArray(R.array.facebook_permissions)));
            else
                new LogoutFromTreasureSeekTask().execute();

        }
    }

    private  void showConnectionError() {
        Toast.makeText(getApplicationContext(), R.string.connectionError, Toast.LENGTH_LONG).show();
    }

    private boolean checkIpAddress(CharSequence text) {

        Matcher matcher = ipPattern.matcher(text);
        return matcher.matches();

    }

    private class IpTextViewListener implements TextView.OnKeyListener {



        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            loginButton.setEnabled(checkIpAddress(((TextView)v).getText()));
            return false;
        }
    }

}
