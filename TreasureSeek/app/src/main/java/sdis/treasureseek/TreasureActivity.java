package sdis.treasureseek;

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

import org.json.JSONException;

import java.io.IOException;

import sdis.controller.Controller;
import sdis.model.Treasure;
import sdis.util.NoAvailableServer;
import sdis.util.ParseMessageException;

public class TreasureActivity extends AppCompatActivity implements View.OnClickListener, TextWatcher {

    TextView tvTreasureDescription;
    TextView tvTreasureAnswer;
    Button buttonSendAnswer;
    ProgressBar progressBar;

    Controller controller = Controller.getInstance();
    Treasure treasure;
    int treasureIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_treasure);
        TreasureActivity.this.setTitle(getString(R.string.found_treasure));

        this.treasureIndex = getIntent().getIntExtra("treasureIndex",0);
        this.treasure = controller.getAllTreasures().get(treasureIndex);

        tvTreasureDescription = findViewById(R.id.text_view_desc);
        tvTreasureDescription.setText((String) treasure.getValue("description"));

        tvTreasureAnswer = findViewById(R.id.text_view_answer);
        tvTreasureAnswer.addTextChangedListener(this);

        buttonSendAnswer = findViewById(R.id.button_sendAnswer);
        buttonSendAnswer.setOnClickListener(this);

        progressBar = findViewById(R.id.sendTreasureProgressBar);

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

        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected Boolean doInBackground(Void... voids) {

            boolean result = false;

            try {
                controller.getAvailableServer();
                result = controller.sendFoundTreasure(treasureIndex, String.valueOf(tvTreasureAnswer.getText()));
            } catch (JSONException | ParseMessageException | IOException | NoAvailableServer e) {

            }

            return result;

        }

        @Override
        protected void onPostExecute(Boolean result) {

            progressBar.setVisibility(View.INVISIBLE);

            if(result == true) {

                tvTreasureDescription.setText(tvTreasureAnswer.getText()+"\n Correct!!");

            }
            else {
                tvTreasureDescription.setText(tvTreasureAnswer.getText()+"\n Wrong!!");
            }


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
