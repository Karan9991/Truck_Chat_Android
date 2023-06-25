package com.teletype.truckchat.ui.road;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.View;
import android.widget.ListView;

import com.teletype.truckchat.ui.common.OnFragmentInteractionListener;
import com.teletype.truckchat.services.RoadClosuresIntentService;

public class RoadClosuresFragment extends ListFragment {

    private OnFragmentInteractionListener mListener;
    private RoadClosuresAdapter mAdapter;

    public static RoadClosuresFragment newInstance(Bundle args) {
        RoadClosuresFragment fragment = new RoadClosuresFragment();
        if (args != null) {
            fragment.setArguments(args);
        }
        return fragment;
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);

        RoadClosuresIntentService.startActionGetRoadClosures(getContext());

        mAdapter = new RoadClosuresAdapter(getActivity());
        mAdapter.loadData();
        setListAdapter(mAdapter);

        ListView listView = getListView();
        listView.setDivider(null);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            mListener = (OnFragmentInteractionListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;

        if (mAdapter != null) {
            mAdapter.unloadData();
            mAdapter = null;
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        if (mListener != null && mAdapter != null) {
            mListener.onFragmentInteractionItemClick(mAdapter.getRoadClosure(position));
        }
    }

}
