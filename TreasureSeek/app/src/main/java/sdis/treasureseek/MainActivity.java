package sdis.treasureseek;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.Profile;

import com.facebook.ProfileTracker;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;

public class MainActivity extends AppCompatActivity {

    CallbackManager facebookCallbackManager = CallbackManager.Factory.create();
    LoginButton loginButton;
    TextView usernameTextView;
    AccessToken facebookAccessToken;
    ProfileListener profileListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loginButton = findViewById(R.id.login_button);
        loginButton.setReadPermissions(this.getResources().getStringArray(R.array.facebook_permissions));
        loginButton.registerCallback(facebookCallbackManager, new TreasureSeekFacebookCallback());


        usernameTextView = this.findViewById(R.id.user_name);
        profileListener = new ProfileListener();

        if(AccessToken.getCurrentAccessToken() != null) {

            if(Profile.getCurrentProfile() != null)
                usernameTextView.setText(Profile.getCurrentProfile().getName());

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
}
