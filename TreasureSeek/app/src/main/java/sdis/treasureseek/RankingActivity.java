package sdis.treasureseek;

import android.content.Context;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import sdis.controller.Controller;
import sdis.util.ParseMessageException;

public class RankingActivity extends AppCompatActivity {

    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private Controller controller;

    JSONArray ranking = new JSONArray();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ranking);

        setTitle(R.string.rankingList);

        controller = Controller.getInstance();

        mRecyclerView = findViewById(R.id.ranking_recycler_view);
        mRecyclerView.setHasFixedSize(true);

        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);



        new GetRankingTask().execute();

    }

    class GetRankingTask extends AsyncTask<Void,Void,Void> {

        @Override
        protected Void doInBackground(Void... voids) {

            try {
                RankingActivity.this.ranking = controller.getRanking();
            } catch (JSONException | ParseMessageException | IOException e) {

            }

            return null;
        }

        @Override
        protected void onPostExecute(Void voids) {

            mAdapter = new RankingActivity.RankingListAdapter(ranking);
            mRecyclerView.setAdapter(mAdapter);
        }
    }


    private class RankingListAdapter extends RecyclerView.Adapter<RankingActivity.RankingListAdapter.ViewHolder> {

        JSONArray mDataSet;

        public RankingListAdapter(JSONArray mDataSet) {
            this.mDataSet = mDataSet;
        }

        @Override
        public RankingActivity.RankingListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            Context context = parent.getContext();
            int layoutIdForListItem = R.layout.ranking_item;
            LayoutInflater inflater = LayoutInflater.from(context);
            boolean shouldAttachImm = false;

            View view = inflater.inflate(layoutIdForListItem, parent, shouldAttachImm);

            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(RankingActivity.RankingListAdapter.ViewHolder holder, int position) {

            holder.bind(position);

        }

        @Override
        public int getItemCount() {
            int size = mDataSet.length();
            return size;
        }

        public class ViewHolder extends RecyclerView.ViewHolder {

            public TextView mTVRankingPosition;
            public TextView mTVRankingPlayerName;
            public TextView mTVRankingNumberOfTreasures;

            public ViewHolder(View itemView) {
                super(itemView);
                mTVRankingPosition = itemView.findViewById(R.id.ranking_number_item);
                mTVRankingPlayerName = itemView.findViewById(R.id.ranking_item_name);
                mTVRankingNumberOfTreasures = itemView.findViewById(R.id.ranking_number_of_treasures);

            }

            void bind(int index) {


                try {
                    JSONObject json = mDataSet.getJSONObject(index);

                    mTVRankingPosition.setText(String.valueOf(index + 1));
                    mTVRankingPlayerName.setText(json.getString("name"));
                    mTVRankingNumberOfTreasures.setText(String.valueOf(json.getString("score")));

                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }

    }

}
