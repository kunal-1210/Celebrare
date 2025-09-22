package com.example.celebrare;
import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

public class FontSpinnerAdapter extends ArrayAdapter<String> {

    private final Context context;
    private final String[] fontNames;
    private final int[] fontResIds;

    public FontSpinnerAdapter(@NonNull Context context, String[] fontNames, int[] fontResIds) {
        super(context, android.R.layout.simple_spinner_item, fontNames);
        this.context = context;
        this.fontNames = fontNames;
        this.fontResIds = fontResIds;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        TextView tv = (TextView) super.getView(position, convertView, parent);
        Typeface tf = ResourcesCompat.getFont(context, fontResIds[position]);
        if (tf != null) tv.setTypeface(tf);

        tv.setText(fontNames[position]);
        return tv;
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        TextView tv = (TextView) super.getDropDownView(position, convertView, parent);
        Typeface tf = ResourcesCompat.getFont(context, fontResIds[position]);
        if (tf != null) tv.setTypeface(tf);
        tv.setPadding(16, 12, 16, 12); // spacing for dropdown
        tv.setTextSize(18);
        tv.setText(fontNames[position]);
        return tv;
    }
}
