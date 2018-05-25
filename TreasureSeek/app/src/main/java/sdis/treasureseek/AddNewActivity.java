package sdis.treasureseek;

import android.os.AsyncTask;
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

import org.json.JSONException;

import java.io.IOException;

import sdis.controller.Controller;
import sdis.util.ParseMessageException;

public class AddNewActivity extends AppCompatActivity implements TextWatcher {

    EditText mTVDesc;
    EditText mTVChallenge;
    EditText mTVAnswer;
    Button mConfirmButton;
    ProgressBar progressBar;

    Controller controller;

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

        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected Boolean doInBackground(Void... voids) {

            boolean result = false;

            String desc = String.valueOf(mTVDesc.getText());
            String challenge = String.valueOf(mTVChallenge.getText());
            String answer = String.valueOf(mTVAnswer.getText());
            double lat = 41.177268;
            double lon = -8.594565;


            try {
                result = controller.insertNewTreasure(desc,challenge,answer,lat,lon);
            }
            catch (JSONException | ParseMessageException | IOException e) {
                System.out.println(e.getLocalizedMessage());
            }

            return result;
        }


        @Override
        protected void onPostExecute(Boolean aVoid) {

            Toast.makeText(getApplicationContext(),"Inserted",Toast.LENGTH_LONG);
            progressBar.setVisibility(View.INVISIBLE);

        }
    }

}
