package com.teletype.truckchat.ui.news;


import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.teletype.truckchat.R;
import com.teletype.truckchat.services.ChatServerAPI;


public class NewsFragment extends Fragment {

    public static NewsAdapter adapter;
    public RecyclerView recyclerView;
    public NewsResults newsResults;
    private ProgressBar progressBar;
    private TextView tvError;

    public static NewsFragment newInstance(Bundle args) {
        NewsFragment fragment = new NewsFragment();
        if (args != null) {
            fragment.setArguments(args);
        }
        return fragment;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_news, container, false);
        recyclerView = view.findViewById(R.id.rvNews);
        progressBar = view.findViewById(R.id.progress_bar);
        tvError = view.findViewById(R.id.tvError);

        makeRequest();

        return view;
    }

    public void makeRequest() {
        ChatServerAPI.GetNewsAsyncTask getNewsAsyncTask = new ChatServerAPI.GetNewsAsyncTask(newsResults);
        getNewsAsyncTask.execute();
    }


    public void initAdapter(ChatServerAPI.GetNewsAsyncTask.NewsResult newsRes) {
        progressBar.setVisibility(View.GONE);

        if (newsRes.getMessage() != null) {
            tvError.setVisibility(View.VISIBLE);
            tvError.setText(newsRes.getMessage());
        } else {
            tvError.setVisibility(View.GONE);
            recyclerView.setHasFixedSize(true);
            LinearLayoutManager llm = new LinearLayoutManager(getContext());
            recyclerView.setLayoutManager(llm);
            adapter = new NewsAdapter(newsRes.getItems(), getString(R.string.posted_on));
            recyclerView.setAdapter(adapter);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            newsResults = (NewsResults) context;
        } catch (ClassCastException e) {
            e.getMessage();
        }
    }

    public interface NewsResults {
        void onNewsResults(ChatServerAPI.GetNewsAsyncTask.NewsResult news);
    }
}
