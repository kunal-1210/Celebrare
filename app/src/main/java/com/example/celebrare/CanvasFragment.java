package com.example.celebrare;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class CanvasFragment extends Fragment {

    private static final String ARG_CANVAS_DATA = "canvas_data";

    private CustomCanvasView customCanvasView;
    private CanvasData canvasData;
    private TextView tvSheetTitle;

    public static CanvasFragment newInstance(CanvasData data) {
        CanvasFragment fragment = new CanvasFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_CANVAS_DATA, data);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Nothing needed here for now
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_canvas, container, false);
        customCanvasView = view.findViewById(R.id.customCanvasView);

        // Restore CanvasData every time
        if (getArguments() != null) {
            canvasData = (CanvasData) getArguments().getSerializable(ARG_CANVAS_DATA);
            if (canvasData != null) {
                customCanvasView.setSheetId((int) canvasData.getId());
                customCanvasView.setTextItems(canvasData.getTextItems());
            }
        }
        Log.d("CanvasFragment", "onCreateView: sheetId=" + getCanvasId() + ", canvas=" + customCanvasView);
        return view;
    }
    public void updateSheetName(String name) {
        if (tvSheetTitle != null) {
            tvSheetTitle.setText(name);
        }
    }

    public CustomCanvasView getCustomCanvas() {
        return customCanvasView;
    }

    public long getCanvasId() {
        return canvasData != null ? canvasData.getId() : 0;
    }

    public CanvasData getCanvasData() {
        return canvasData;
    }
}
