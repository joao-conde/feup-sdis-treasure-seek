package sdis.treasureseek;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONException;

import java.io.IOException;

import sdis.controller.Controller;
import sdis.model.Treasure;
import sdis.util.ParseMessageException;

public class AddNewActivity extends AppCompatActivity implements TextWatcher {

    private final int REQUEST_LOCATION = 1;

    EditText mTVDesc;
    EditText mTVChallenge;
    EditText mTVAnswer;
    Button mConfirmButton;
    ProgressBar progressBar;

    Controller controller;

    private FusedLocationProviderClient mFusedLocationClient;


    double lat = 0;
    double lon = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_new);

        setTitle(R.string.new_treasure_title);

        controller = Controller.getInstance();

        mTVDesc = findViewById(R.id.tv_new_treasure_description);
        mTVChallenge = findViewById(R.id.tv_new_treasure_challenge);
        mTVAnswer = findViewById(R.id.tv_new_treasure_answer);
        mConfirmButton = findViewById(R.id.btn_add_new_treasure);
        progressBar = findViewById(R.id.pg_new_treasure);

        mTVDesc.addTextChangedListener(this);
        mTVChallenge.addTextChangedListener(this);
        mTVAnswer.addTextChangedListener(this);
        mConfirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                new SendNewTreasure().execute();

            }
        });

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION);
        }


    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {}

    @Override
    public void afterTextChanged(Editable s) {
        mConfirmButton.setEnabled(updateButton());
    }

    private boolean updateButton() {

        return !String.valueOf(mTVDesc.getText()).trim().equals("")
                && !String.valueOf(mTVChallenge.getText()).trim().equals("")
                && !String.valueOf(mTVAnswer.getText()).trim().equals("");


    }

    class SendNewTreasure extends AsyncTask<Void,Void,Boolean> {

        @SuppressLint("MissingPermission")
        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);

            mFusedLocationClient.getLastLocation()
                    .addOnSuccessListener(AddNewActivity.this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {

                            if (location != null) {

                                AddNewActivity.this.lat = location.getLatitude();
                                AddNewActivity.this.lon = location.getLongitude();

                            }
                        }
                    });

        }

        @SuppressLint("MissingPermission")
        @Override
        protected Boolean doInBackground(Void... voids) {

            boolean result = false;

            String desc = String.valueOf(mTVDesc.getText());
            String challenge = String.valueOf(mTVChallenge.getText());
            String answer = String.valueOf(mTVAnswer.getText());


            try {
                while(lat == 0) continue;
                result = controller.insertNewTreasure(desc,challenge,answer,lat,lon);
            }
            catch (JSONException | ParseMessageException | IOException e) {
                System.out.println(e.getLocalizedMessage());
            }

            return result;
        }


        @SuppressLint("MissingPermission")
        @Override
        protected void onPostExecute(Boolean result) {

            progressBar.setVisibility(View.INVISIBLE);

            if(result == false) {
                Toast.makeText(getApplicationContext(),"Inserted",Toast.LENGTH_LONG);
                return;
            }




            Treasure treasure = new Treasure(0, lat, lon, String.valueOf(mTVDesc.getText()),
                    String.valueOf(mTVChallenge.getText()), String.valueOf(mTVAnswer.getText()));



            controller.addTreasure(treasure);
            AddNewActivity.this.finish();

        }
    }

}
