package com.teletype.truckchat.ui.replies;

import android.app.ListFragment;
import android.os.Bundle;
import android.widget.AbsListView;
import android.widget.ListView;

import com.teletype.truckchat.util.Constants;

public class RepliesFragment extends ListFragment {

    private RepliesAdapter mAdapter;

    public static RepliesFragment newInstance(Bundle args) {
        RepliesFragment fragment = new RepliesFragment();
        if (args != null) {
            fragment.setArguments(args);
        }
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = getArguments();
        if (bundle != null) {
            String conversationId = bundle.getString(Constants.BROADCAST_EXTRA_CONVERSATION_ID);
            String userId = bundle.getString(Constants.BROADCAST_EXTRA_USER_ID);
            boolean ignoreAutoHide = bundle.getBoolean(Constants.BROADCAST_EXTRA_IGNORE_AUTOHIDE, false);
            String emoji_id = bundle.getString(Constants.EMOJI_ID);
            mAdapter = new RepliesAdapter(getActivity(), conversationId, userId, ignoreAutoHide, emoji_id);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setListAdapter(mAdapter);
        ListView listView = getListView();
        mAdapter.loadData();
        listView.setDivider(null);
        listView.setTranscriptMode(AbsListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
        listView.setStackFromBottom(true);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mAdapter.unloadData();
    }

}
