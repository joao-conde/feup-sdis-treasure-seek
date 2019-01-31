package sdis.treasureseek;

import android.graphics.Color;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;

import sdis.controller.Controller;
import sdis.model.Treasure;
import sdis.util.NoAvailableServer;
import sdis.util.ParseMessageException;
import sdis.util.TreasureSeekException;

public class TreasureActivity extends AppCompatActivity implements View.OnClickListener, TextWatcher {

    TextView tvTreasureDescription;
    TextView tvTreasureAnswer;
    TextView tvTreasureQuestion;
    TextView tvResult;
    Button buttonSendAnswer;
    ProgressBar progressBar;

    Controller controller = Controller.getInstance();
    Treasure treasure;
    int treasureIndex;
    boolean found;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_treasure);
        TreasureActivity.this.setTitle(getString(R.string.found_treasure));

        this.found = getIntent().getBooleanExtra("found",false);
        this.treasureIndex = getIntent().getIntExtra("treasureIndex",0);
        this.treasure =  !found ? controller.getAllTreasures().get(treasureIndex) : ((ArrayList<Treasure>)controller.getLoggedUser().getValue("foundTreasures")).get(treasureIndex);

        tvTreasureDescription = findViewById(R.id.text_view_desc);
        tvTreasureDescription.setText((String) treasure.getValue("description"));

        tvTreasureAnswer = findViewById(R.id.text_view_answer);
        tvTreasureQuestion = findViewById(R.id.text_view_answer_title);
        tvResult = findViewById(R.id.text_view_result);

        buttonSendAnswer = findViewById(R.id.button_sendAnswer);
        progressBar = findViewById(R.id.sendTreasureProgressBar);

        if(found) {

            tvTreasureAnswer.setEnabled(false);
            tvTreasureQuestion.setText((String)treasure.getValue("challenge"));
            tvTreasureAnswer.setText((String)treasure.getValue("answer"));
            tvResult.setText(getString(R.string.correctAnswer));
            tvResult.setTextColor(Color.GREEN);

        }

        else {

            tvTreasureAnswer.addTextChangedListener(this);
            buttonSendAnswer.setOnClickListener(this);
            tvResult.setTextColor(Color.RED);
        }


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        menu.getItem(0).setTitle((String)controller.getLoggedUser().getValue("name"));
        return super.onCreateOptionsMenu(menu);

    }

    @Override
    public void onClick(View v) {

        new SendFoundTreasure().execute();

    }



    private boolean checkAnswerNotEmpty() {

        return !String.valueOf(tvTreasureAnswer.getText()).trim().equals("");

    }

    class SendFoundTreasure extends AsyncTask<Void,Void,Boolean> {

        boolean wrong = false;
        String error = "";

        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected Boolean doInBackground(Void... voids) {

            boolean result = false;
            boolean somethingWrong = false;

            try {
                controller.getAvailableServer();
                result = controller.sendFoundTreasure(treasureIndex, String.valueOf(tvTreasureAnswer.getText()));
            } catch (JSONException | IOException | TreasureSeekException e) {

                wrong = true;
                error = e.getLocalizedMessage();

            }

            return result;

        }

        @Override
        protected void onPostExecute(Boolean result) {

            progressBar.setVisibility(View.INVISIBLE);

            if(wrong) {

                Toast.makeText(getApplicationContext(),error,Toast.LENGTH_LONG).show();
                this.cancel(true);
                progressBar.setVisibility(View.INVISIBLE);
                return;
            }

            if(result == true) {

                Toast.makeText(getApplicationContext(),getString(R.string.correctAnswer),Toast.LENGTH_LONG).show();
                TreasureActivity.this.finish();

            }
            else
                tvResult.setText(R.string.wrongAnswer);

        }
    }


    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) { }

    @Override
    public void afterTextChanged(Editable s) {
        buttonSendAnswer.setEnabled(checkAnswerNotEmpty());
    }
}
