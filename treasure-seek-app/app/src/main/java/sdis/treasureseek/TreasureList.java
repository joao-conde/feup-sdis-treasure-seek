package sdis.treasureseek;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

import sdis.controller.Controller;
import sdis.model.Treasure;

public class TreasureList extends AppCompatActivity {

    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    private Controller controller;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_treasure_list);

        setTitle(R.string.treasureListTitle);

        controller = Controller.getInstance();

        mRecyclerView = findViewById(R.id.treasure_recycler_view);
        mRecyclerView.setHasFixedSize(true);

        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        mAdapter = new TreasureListAdapter(controller.getAllTreasures());
        mRecyclerView.setAdapter(mAdapter);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.admin_menu, menu);
        return super.onCreateOptionsMenu(menu);

    }

    private class TreasureListAdapter extends RecyclerView.Adapter<TreasureListAdapter.ViewHolder> {

        ArrayList<Treasure> mDataSet;

        public TreasureListAdapter(ArrayList<Treasure> dataSet) {

            mDataSet = dataSet;

        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

           Context context = parent.getContext();
           int layoutIdForListItem = R.layout.treasure_item;
           LayoutInflater inflater = LayoutInflater.from(context);
           boolean shouldAttachImm = false;

           View view = inflater.inflate(layoutIdForListItem, parent, shouldAttachImm);

           return new ViewHolder(view);

        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.bind(position);
        }

        @Override
        public int getItemCount() {
            return this.mDataSet.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {

            public TextView mTVTreasureDescription;
            public TextView mTVTreasureLatitude;
            public TextView mTVTreasureLongitude;

            public ViewHolder(View itemView) {
                super(itemView);
                mTVTreasureDescription = itemView.findViewById(R.id.treasure_item_desc);
                mTVTreasureLatitude = itemView.findViewById(R.id.treasure_item_latitude);
                mTVTreasureLongitude = itemView.findViewById(R.id.treasure_item_longitude);

            }

            void bind(int index) {

                mTVTreasureDescription.setText((String)  mDataSet.get(index).getValue("description"));
                mTVTreasureLatitude.setText(String.valueOf(mDataSet.get(index).getValue("latitude")));
                mTVTreasureLongitude.setText(String.valueOf(mDataSet.get(index).getValue("longitude")));

            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if(item.getItemId() == R.id.addTreasure) {
            Intent intent = new Intent(TreasureList.this, AddNewActivity.class);
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onRestart() {

        super.onRestart();
        mAdapter.notifyDataSetChanged();
    }
}
