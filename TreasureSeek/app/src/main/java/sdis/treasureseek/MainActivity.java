package sdis.treasureseek;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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


import java.io.IOException;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import sdis.controller.Controller;
import sdis.util.NoAvailableServer;
import sdis.util.ParseMessageException;


public class MainActivity extends AppCompatActivity {

    CallbackManager facebookCallbackManager = CallbackManager.Factory.create();

    Button loginButton;
    ProgressBar progressBar;
    TextView ipTextView;

    LoginManager fbLoginManager;
    AccessToken facebookAccessToken;

    Controller controller;

    private static final Pattern ipPattern = Pattern.compile("^([0-9]{1,3}\\.){3}[0-9]{1,3}$");;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        controller = Controller.getInstance(getApplicationContext());

        fbLoginManager = LoginManager.getInstance();
        fbLoginManager.registerCallback(facebookCallbackManager, new TreasureSeekFacebookCallback());

        loginButton = findViewById(R.id.login_button);
        loginButton.setOnClickListener(new LoginListener());

        progressBar = findViewById(R.id.loginProgressBar);
        progressBar.setVisibility(View.GONE);

        ipTextView = findViewById(R.id.textIp);
        ipTextView.setOnKeyListener(new IpTextViewListener());

        this.facebookAccessToken = AccessToken.getCurrentAccessToken();

        ipTextView.setText("192.168.1.105");
        loginButton.setEnabled(true);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        facebookCallbackManager.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }


    private class LoginToTreasureSeekTask extends AsyncTask<Void,Void,Boolean> {


        @Override
        protected Boolean doInBackground(Void... voids) {

            boolean result = false;

            try  {
                result = controller.loginToTreasureSeek();
            }

            catch (IOException | ParseMessageException | JSONException e) {
                System.out.println(e.getLocalizedMessage());
            }

            return result;
        }

        @Override
        protected void onPostExecute(Boolean result) {

            if(result == true)
                navigateToMap();

            else {
                showConnectionError();
                fbLoginManager.logOut();
            }

        }
    }

    private class LogoutFromTreasureSeekTask extends AsyncTask<Void,Void,Boolean> {


        @Override
        protected Boolean doInBackground(Void... voids) {

            boolean result = false;

            try  {
                result = controller.logoutFromTreasureSeek();
            }

            catch (IOException | ParseMessageException | JSONException e) {
                System.out.println(e.getLocalizedMessage());
            }

            return result;
        }

        @Override
        protected void onPostExecute(Boolean result) {

            progressBar.setVisibility(View.GONE);

            if(result == true) {
                loginButton.setText(getString(R.string.login));
                fbLoginManager.logOut();
            }


            else
                showConnectionError();

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
            new LoginToTreasureSeekTask().execute();

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

    private class RequestAvailableServerTask extends AsyncTask<Void,Void,Boolean> {

        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
            controller.setLoadBalancerAddress(String.valueOf(ipTextView.getText()));
        }

        @Override
        protected Boolean doInBackground(Void... voids) {

            boolean result = false;

            try {
                result = controller.getAvailableServer();


            } catch (IOException | JSONException | ParseMessageException | NoAvailableServer e) {
                e.printStackTrace();
            }

            return result;

        }

        @Override
        protected void onPostExecute(Boolean result) {

            if(result == false) {
                showConnectionError();
                progressBar.setVisibility(View.GONE);
                return;

            }



            if(controller.isLogged()) {

                if(loginButton.getText().equals(getString(R.string.login)))
                    new LoginToTreasureSeekTask().execute();
                else
                    new LogoutFromTreasureSeekTask().execute();
            }

            else
                fbLoginManager.logInWithReadPermissions(MainActivity.this, Arrays.asList(getResources().getStringArray(R.array.facebook_permissions)));

        }
    }

    private  void showConnectionError() {
        progressBar.setVisibility(View.GONE);
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

    private void navigateToMap() {


        loginButton.setText(getString(R.string.logout));
        Intent intent = new Intent(MainActivity.this, TreasureMapActivity.class);
        startActivity(intent);
        progressBar.setVisibility(View.GONE);



    }

}
