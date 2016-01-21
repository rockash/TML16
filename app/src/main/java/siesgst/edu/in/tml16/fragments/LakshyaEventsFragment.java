package siesgst.edu.in.tml16.fragments;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import siesgst.edu.in.tml16.R;
import siesgst.edu.in.tml16.adapters.EventAdapter;

/**
 * A simple {@link Fragment} subclass.
 */
public class LakshyaEventsFragment extends Fragment {

    private RecyclerView recyclerView;
    private EventAdapter adapter;


    public LakshyaEventsFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_lakshya_event, container, false);

        recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        adapter = new EventAdapter(getActivity(), "Lakshya");
        recyclerView.setAdapter(adapter);

        return view;
    }

}