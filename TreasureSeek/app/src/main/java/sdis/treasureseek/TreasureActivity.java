package sdis.treasureseek;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import sdis.controller.Controller;

public class TreasureActivity extends AppCompatActivity {

    TextView treasureDescription;
    Controller controller = Controller.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_treasure);

        int index = getIntent().getIntExtra("treasureIndex",0);
        treasureDescription = findViewById(R.id.text_view_desc);
        treasureDescription.setText((String) controller.getAllTresoures().get(index).getValue("description"));

    }
}
