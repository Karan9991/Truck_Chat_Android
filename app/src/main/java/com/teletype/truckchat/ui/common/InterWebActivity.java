package com.teletype.truckchat.ui.common;

import android.app.ActionBar;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.Window;
import android.webkit.WebView;

import com.teletype.truckchat.R;
import com.teletype.truckchat.ui.conversations.ConversationsFragment;
import com.teletype.truckchat.util.Constants;


public class InterWebActivity extends FragmentActivity implements ConversationsFragment.InterWebFragment.OnFragmentInterWebListener {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.activity_interweb);
        setProgressBarIndeterminateVisibility(false);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        FragmentManager fm = getSupportFragmentManager();
        Fragment fragment = fm.findFragmentById(R.id.interWebActivity_frameLayout);
        if (fragment == null) {
            Intent intent = getIntent();
            String url = intent.getStringExtra(Constants.EXTRA_URI);

            Bundle bundle = null;
            if (!TextUtils.isEmpty(url)) {
                bundle = new Bundle();
                bundle.putString(Constants.EXTRA_URI, url);
            }

            fragment = ConversationsFragment.InterWebFragment.newInstance(bundle);
            fm.beginTransaction().add(R.id.interWebActivity_frameLayout, fragment).commit();
        }
	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case android.R.id.home:
                finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        setProgressBarIndeterminateVisibility(true);
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        setProgressBarIndeterminateVisibility(false);
    }

    @Override
    public void onBackPressed() {
        FragmentManager fm = getSupportFragmentManager();
        Fragment fragment = fm.findFragmentById(R.id.interWebActivity_frameLayout);
        if (fragment != null && fragment instanceof ConversationsFragment.InterWebFragment) {
            ConversationsFragment.InterWebFragment webFragment = (ConversationsFragment.InterWebFragment) fragment;
            if (webFragment.goBack()) {
                return;
            }
        }

        super.onBackPressed();
    }
}
