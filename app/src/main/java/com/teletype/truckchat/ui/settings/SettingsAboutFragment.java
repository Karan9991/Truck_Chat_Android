package com.teletype.truckchat.ui.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.teletype.truckchat.ui.common.InterWebActivity;
import com.teletype.truckchat.R;
import com.teletype.truckchat.util.Constants;
import com.teletype.truckchat.util.Utils;


public class SettingsAboutFragment extends Fragment {

    private TextView mTextViewVersion;
    private TextView mTextViewSerial;
    private TextView mTextViewVisit;
    private TextView mTextViewContact;
    private TextView mTextViewTellAFriend;
    private TextView mTextViewTermsOfService;
    //private TextView mTextViewLegalNotices;

    /*public static SettingsAboutFragment newInstance(Bundle args) {
        SettingsAboutFragment fragment = new SettingsAboutFragment();
        if (args != null) {
            fragment.setArguments(args);
        }
        return fragment;
    }*/

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_about, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mTextViewVersion = (TextView) view.findViewById(R.id.aboutFragment_textView_version);
        mTextViewSerial = (TextView) view.findViewById(R.id.aboutFragment_textView_serial);
        mTextViewVisit = (TextView) view.findViewById(R.id.aboutFragment_textView_visit);
        mTextViewContact = (TextView) view.findViewById(R.id.aboutFragment_textView_contact);
        mTextViewTellAFriend = (TextView) view.findViewById(R.id.aboutFragment_textView_tell_a_friend);
        mTextViewTermsOfService = (TextView) view.findViewById(R.id.aboutFragment_textView_tos);
        //mTextViewLegalNotices = (TextView) view.findViewById(R.id.aboutFragment_textView_legal_notices);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Activity activity = getActivity();

        mTextViewVersion.setText(getString(R.string.about_version, Utils.getAppVersionName(activity)));
        mTextViewSerial.setText(getString(R.string.about_serial, Utils.getPaddedSerialNumber(activity)));

        MovementMethod mm = LinkMovementMethod.getInstance();

        mTextViewVisit.setMovementMethod(mm);
        Spannable s = (Spannable)mTextViewVisit.getText();
        URLSpan[] u = s.getSpans(0, s.length(), URLSpan.class);
        if (u != null && u.length > 0) {
            final String url = u[0].getURL();
            s.setSpan(new ClickableSpan() {
                @Override
                public void onClick(View widget) {
                    startActivity(new Intent(getActivity(), InterWebActivity.class)
                                    .putExtra(Constants.EXTRA_URI, url));
                }
            }, s.getSpanStart(u[0]), s.getSpanEnd(u[0]), s.getSpanFlags(u[0]));
            s.removeSpan(u[0]);
        }

        mTextViewContact.setMovementMethod(mm);
        s = (Spannable)mTextViewContact.getText();
        u = s.getSpans(0, s.length(), URLSpan.class);
        if (u != null && u.length > 0) {
            final String url = u[0].getURL();
            s.setSpan(new ClickableSpan() {
                @Override
                public void onClick(View widget) {
                    startActivity(new Intent(getActivity(), InterWebActivity.class)
                            .putExtra(Constants.EXTRA_URI, url));
                }
            }, s.getSpanStart(u[0]), s.getSpanEnd(u[0]), s.getSpanFlags(u[0]));
            s.removeSpan(u[0]);
        }

        mTextViewTellAFriend.setMovementMethod(mm);
        s = (Spannable)mTextViewTellAFriend.getText();
        u = s.getSpans(0, s.length(), URLSpan.class);
        if (u != null && u.length > 0) {
            s.setSpan(new ClickableSpan() {
                @Override
                public void onClick(View widget) {
                    Utils.composeTellAFriendEmail(getActivity());
                }
            }, s.getSpanStart(u[0]), s.getSpanEnd(u[0]), s.getSpanFlags(u[0]));
            s.removeSpan(u[0]);
        }

        mTextViewTermsOfService.setMovementMethod(mm);
        s = (Spannable) mTextViewTermsOfService.getText();
        s.setSpan(new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                Activity activity = getActivity();
                if (activity != null) {
                    new AlertDialog.Builder(activity)
                            .setTitle(R.string.about_tos)
                            .setMessage(R.string.about_tos_body)
                            .setCancelable(false)
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                }
            }}, 0, s.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        /*
        mTextViewLegalNotices.setMovementMethod(mm);
        s = (Spannable) mTextViewLegalNotices.getText();
        s.setSpan(new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                Activity activity = getActivity();
                if (activity != null) {
                    new AlertDialog.Builder(activity)
                            .setTitle_posred(R.string.about_legal_notices)
                            .setMessage(GoogleApiAvailability.getInstance().getOpenSourceSoftwareLicenseInfo(activity))
                            .setCancelable(false)
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                }
            }
        }, 0, s.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        */

    }

}
