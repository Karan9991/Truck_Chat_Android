package com.teletype.truckchat.ui.settings;


import android.content.Context;
import android.content.res.TypedArray;
//import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.teletype.truckchat.R;

public class AdapterAvatar extends ArrayAdapter<String> {
    private TypedArray data;
    
    private static class ViewHolder {
        ImageView ivAvatar;
    }

    AdapterAvatar(String[] strings, TypedArray data, Context context) {
        super(context, R.layout.row_item, strings);
        this.data = data;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {

        ViewHolder viewHolder;

        if (convertView == null) {
            viewHolder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.row_item, parent, false);
            convertView.setTag(viewHolder);
            viewHolder.ivAvatar = convertView.findViewById(R.id.ivAvatar);

        }else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        viewHolder.ivAvatar.setImageDrawable(data.getDrawable(position));

        return convertView;
    }
}

