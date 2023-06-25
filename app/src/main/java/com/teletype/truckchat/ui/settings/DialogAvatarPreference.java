package com.teletype.truckchat.ui.settings;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;

import com.teletype.truckchat.R;
import com.teletype.truckchat.util.Utils;



public class DialogAvatarPreference extends DialogFragment implements AdapterView.OnItemClickListener {

    private ListView myList;
    private TypedArray avatars;
    public static final String IS_CLOSE_VISIBLE = "IS_CLOSE_VISIBLE";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.choose_avatar_dialog, null, false);
        myList =  view.findViewById(R.id.listAvatar);

        ImageView ivClose = view.findViewById(R.id.ivClose);

        Bundle args = getArguments();
        boolean isCloseVisible = args.getBoolean(IS_CLOSE_VISIBLE);
        if(isCloseVisible) {
            ivClose.setVisibility(View.VISIBLE);
        } else {
            ivClose.setVisibility(View.GONE);
        }
        ivClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {

        super.onActivityCreated(savedInstanceState);

        avatars = getActivity().getResources().obtainTypedArray(R.array.avatars);
        String[] arr = new String[avatars.length()];
        AdapterAvatar adapter = new AdapterAvatar(arr, avatars,getActivity());

        myList.setAdapter(adapter);
        myList.setOnItemClickListener(this);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Utils.setAvatar(getActivity(), avatars.getResourceId(position, -1));
        dismiss();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }
}


