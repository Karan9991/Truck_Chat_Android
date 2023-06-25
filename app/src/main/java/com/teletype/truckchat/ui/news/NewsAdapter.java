package com.teletype.truckchat.ui.news;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.teletype.truckchat.R;
import com.teletype.truckchat.services.ChatServerAPI;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;


public class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.NewsViewHolder>{

    private List<ChatServerAPI.GetNewsAsyncTask.NewsItem> news;
    private String posted_on;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public NewsViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_news_list, parent, false);

        return new NewsViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final NewsViewHolder holder, int position) {
        final ChatServerAPI.GetNewsAsyncTask.NewsItem newsItem = news.get(position);
        final RelativeLayout rlRowNewsRoot = holder.rlRowNewsRoot;
        if (newsItem.isAdverb()) {
            holder.llInfo.setVisibility(View.GONE);
            holder.ivNewsIcon.setVisibility(View.GONE);
            rlRowNewsRoot.setBackgroundResource(0);

            final AdView mAdView = holder.adView;
            mAdView.setAdListener(new AdListener() {
                @Override
                public void onAdLoaded() {
                    mAdView.setVisibility(View.VISIBLE);
                }
            });

            Bundle extras = new Bundle();
            extras.putBoolean("is_designed_for_families", false);

            AdRequest adRequest = new AdRequest.Builder()
                    .addNetworkExtrasBundle(AdMobAdapter.class, extras)
                    .addKeyword("truck")
                    .tagForChildDirectedTreatment(false)
                    .build();

            mAdView.loadAd(adRequest);
        } else {
            holder.llInfo.setVisibility(View.VISIBLE);
            holder.ivNewsIcon.setVisibility(View.VISIBLE);

            holder.tvTitle.setText(news.get(position).getTitle());
            String dateParsed = posted_on + " " + formatDate(news.get(position).getPosted_date());
            holder.tvDate.setText(dateParsed);

            holder.rlRowNewsRoot.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(newsItem.getLink()));
                    rlRowNewsRoot.getContext().startActivity(browserIntent);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return news.size();
    }

    class NewsViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivNewsIcon;
        private LinearLayout llInfo;
        private RelativeLayout rlRowNewsRoot;
        private TextView tvTitle;
        private TextView tvDate;
        private AdView adView;

        NewsViewHolder(View itemView) {
            super(itemView);
            rlRowNewsRoot = itemView.findViewById(R.id.rlRowNewsRoot);
            ivNewsIcon = itemView.findViewById(R.id.ivNewsIcon);
            llInfo = itemView.findViewById(R.id.llInfo);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvDate = itemView.findViewById(R.id.tvDate);
            adView = itemView.findViewById(R.id.adView);
        }
    }

    NewsAdapter(List<ChatServerAPI.GetNewsAsyncTask.NewsItem> news, String posted_on) {
        this.news = news;
        this.posted_on = posted_on;
    }

    private String formatDate(String date) {
        String reformattedStr = null;
        if (date != null) {
            try {
                SimpleDateFormat fromUser = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                SimpleDateFormat myFormat;
                myFormat = new SimpleDateFormat("dd/MM/yyyy",
                        Locale.getDefault());
                reformattedStr = myFormat.format(fromUser.parse(date));
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return reformattedStr;
    }
}

