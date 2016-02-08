package siesgst.edu.in.tml16.fragments;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;

import siesgst.edu.in.tml16.EventDetailsActivity;
import siesgst.edu.in.tml16.R;
import siesgst.edu.in.tml16.adapters.EventAdapter;
import siesgst.edu.in.tml16.helpers.ItemClickSupport;
import siesgst.edu.in.tml16.utils.ConnectionUtils;
import siesgst.edu.in.tml16.utils.DataHandler;
import siesgst.edu.in.tml16.utils.LocalDBHandler;
import siesgst.edu.in.tml16.utils.OnlineDBDownloader;

/**
 * A simple {@link Fragment} subclass.
 */
public class MusicanaFragment extends Fragment {

    RecyclerView recyclerView;
    EventAdapter adapter;

    CoordinatorLayout layout;
    SwipeRefreshLayout swipeRefreshLayout;

    public MusicanaFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_musicana, container, false);

        layout = (CoordinatorLayout) view.findViewById(R.id.events_layout);
        recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        adapter = new EventAdapter(getActivity(), "Moksh", "Musicana");
        recyclerView.setAdapter(adapter);
        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.refresh_view);
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimaryDark, R.color.colorAccent);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                onRefreshData();
            }
        });

        ItemClickSupport.addTo(recyclerView).setOnItemClickListener(new ItemClickSupport.OnItemClickListener() {
            @Override
            public void onItemClicked(RecyclerView recyclerView, int position, View v) {
                TextView eventName = (TextView) v.findViewById(R.id.event_name);
                Intent intent = new Intent(getActivity(), EventDetailsActivity.class);
                intent.putExtra("event_name", eventName.getText());
                startActivity(intent);
            }
        });

        return view;
    }

    public void onRefreshData() {
        swipeRefreshLayout.setRefreshing(true);
        new EventListDownload().execute();
    }

    private class EventListDownload extends AsyncTask<Void, Void, JSONArray> {
        JSONArray array;
        SharedPreferences sharedPreferences;

        @Override
        protected void onPreExecute() {
            if (new ConnectionUtils(getActivity()).checkConnection()) {
                new LocalDBHandler(getActivity()).dropEventsTable();
            } else {
                Snackbar.make(layout, "Can't connect to network..", Snackbar.LENGTH_INDEFINITE).setAction("Try Again", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onRefreshData();
                    }
                }).show();
            }
        }

        @Override
        protected JSONArray doInBackground(Void... params) {
            sharedPreferences = getActivity().getSharedPreferences("TML", Context.MODE_PRIVATE);
            OnlineDBDownloader downloader = new OnlineDBDownloader(getActivity());
            downloader.downloadData();
            array = downloader.getJSON();
            if (!sharedPreferences.getString("nw_status", "").equals("bad")) {
                new DataHandler(getActivity()).decodeAndPushJSON(array);
            } else {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getActivity(), "Check your internet connection...", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            return array;
        }

        @Override
        protected void onPostExecute(JSONArray jsonArray) {
            adapter = new EventAdapter(getActivity(), "Moksh", "Musicana");
            recyclerView.setAdapter(adapter);
            swipeRefreshLayout.setRefreshing(false);
        }
    }
}