package com.teletype.truckchat.ui.conversations;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;

import com.teletype.truckchat.ui.common.OnFragmentInteractionListener;
import com.teletype.truckchat.R;
import com.teletype.truckchat.util.Constants;
import com.teletype.truckchat.util.Utils;


public class ConversationsFragment extends ListFragment
        implements AdapterView.OnItemLongClickListener, ConversationsAdapter.OnEmptyDataListener {

    //private static final String TAG = "TAG_" + ConversationsFragment.class.getSimpleName();

    private OnFragmentInteractionListener mListener;
    private ConversationsAdapter mAdapter;

    public static ConversationsFragment newInstance(Bundle args) {
        ConversationsFragment fragment = new ConversationsFragment();
        if (args != null) {
            fragment.setArguments(args);
        }
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mAdapter != null) {
            mAdapter.mIsAfterInitialLoad = false;
        }
    }


    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);

        if (mAdapter != null) {
            mAdapter.unloadData();
        }
        mAdapter = new ConversationsAdapter(getActivity());
        mAdapter.loadData(this);
        setListAdapter(mAdapter);

        ListView listView = getListView();
        listView.setDivider(null);

        listView.setOnItemLongClickListener(this);
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
        if (mListener != null && mAdapter != null) {
            mListener.onFragmentInteractionItemClick(mAdapter.getConversation(position), false);
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        if (mListener != null && mAdapter != null) {
            mListener.onFragmentInteractionItemLongClick(mAdapter.getConversation(position));
            return true;
        }

        return false;
    }

    @Override
    public void onEmptyData() {
        if (mListener != null) {
            mListener.onEmptyData();
        }
    }

    public void actionSelectAll() {
        Activity activity = getActivity();
        if (activity != null) {
            new AlertDialog.Builder(activity)
                    .setItems(R.array.action_select_all_read_unread, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which) {
                                case 0: // Mark all as read
                                    mAdapter.markAll(true);
                                    break;

                                case 1: // Mark as unread
                                    mAdapter.markAll(false);
                                    break;
                            }
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();

        }
    }

    public static class InterWebFragment extends Fragment {

        private static final int REQUEST_CODE = 0x0001;

        private OnFragmentInterWebListener mListener;
        private WebView mWebView;
        private String mUrl;
        private boolean mOpenNewPagesInNewWindow = false;

        public static InterWebFragment newInstance(Bundle args) {
            InterWebFragment fragment = new InterWebFragment();
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
                mUrl = bundle.getString(Constants.EXTRA_URI);
                mOpenNewPagesInNewWindow = bundle.getBoolean(Constants.EXTRA_OPEN_IN_NEW_WINDOW);
            }
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_interweb, container, false);
        }

        @SuppressLint("SetJavaScriptEnabled")
        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            mWebView = (WebView) view.findViewById(R.id.interWebFragment_webView);

            mWebView.getSettings().setJavaScriptEnabled(true);
            mWebView.setWebViewClient(
                    new WebViewClient() {
                        @Override
                        public void onPageStarted(WebView view, String url, Bitmap favicon) {
                            mListener.onPageStarted(view, url, favicon);
                        }

                        @Override
                        public void onPageFinished(WebView view, String url) {
                            mListener.onPageFinished(view, url);
                        }

                        @Override
                        public boolean shouldOverrideUrlLoading(WebView view, String url) {
                            if (url.endsWith(".pdf") || mOpenNewPagesInNewWindow) {
                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                            } else {
                                view.loadUrl(url);
                            }

                            return true;
                        }

                        @Override
                        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                            Activity activity = getActivity();
                            if (activity != null) {
                                AlertDialog dialog = new AlertDialog.Builder(activity)
                                        .setTitle(R.string.error_interweb)
                                        .setMessage(description)
                                        .setPositiveButton(android.R.string.ok, null)
                                        .create();
                                dialog.setCanceledOnTouchOutside(false);
                                dialog.show();
                            }
                        }
                    }
            );

            if (!TextUtils.isEmpty(mUrl)) {
                checkNetwork();
            }

        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            switch (requestCode) {
                case REQUEST_CODE:
                    checkNetwork();
                    break;
            }
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);

            try {
                mListener = (OnFragmentInterWebListener) activity;
            } catch (ClassCastException e) {
                throw new ClassCastException(activity.toString()
                        + " must implement OnFragmentInterWebListener");
            }
        }

        @Override
        public void onDestroy() {
            if (mWebView != null) {
                mWebView.destroy();
            }

            super.onDestroy();
        }

        @Override
        public void onDetach() {
            super.onDetach();
            mListener = null;
        }

        public boolean goBack() {
            if (mWebView != null && mWebView.canGoBack()) {
                mWebView.goBack();
                return true;
            }

            return false;
        }

        public void reloadUrl() {
            if (mWebView != null) {
                mWebView.clearHistory();
                mWebView.clearCache(true);
                mWebView.loadUrl(mUrl);
            }
        }

        public interface OnFragmentInterWebListener {

            void onPageStarted(WebView view, String url, Bitmap favicon);
            void onPageFinished(WebView view, String url);
        }

        private void checkNetwork() {
            Activity activity = getActivity();
            if (activity == null) {
                return;
            }

            if (Utils.isNetworkEnabled(activity)) {
                reloadUrl();
            } else {
                AlertDialog dialog = new AlertDialog.Builder(activity)
                        .setTitle(R.string.enable_data_services)
                        .setMessage(R.string.enable_data_services_desc)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                startActivityForResult(new Intent(Settings.ACTION_WIRELESS_SETTINGS), REQUEST_CODE);
                            }
                        })
                        .setNegativeButton(android.R.string.no, null)
                        .create();
                dialog.setCanceledOnTouchOutside(false);
                dialog.show();
            }
        }

    }
}
