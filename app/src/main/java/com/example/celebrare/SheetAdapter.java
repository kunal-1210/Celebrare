package com.example.celebrare;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Collections;
import java.util.List;

public class SheetAdapter extends RecyclerView.Adapter<SheetAdapter.SheetViewHolder> {

    private List<CanvasData> sheets;
    private OnStartDragListener dragListener;
    private OnDeleteSheetListener deleteListener;
    private OnSheetNameClickListener sheetNameClickListener;

    // Listener interfaces
    public interface OnStartDragListener {
        void onStartDrag(RecyclerView.ViewHolder viewHolder);
    }

    public interface OnDeleteSheetListener {
        void onDelete(int position);
    }
    public interface OnSheetNameClickListener { // ✅ New
        void onSheetNameClick(int position);
    }
    public void setOnSheetNameClickListener(OnSheetNameClickListener listener) { // ✅ New
        this.sheetNameClickListener = listener;
    }

    public SheetAdapter(List<CanvasData> sheets, OnStartDragListener dragListener, OnDeleteSheetListener deleteListener) {
        this.sheets = sheets;
        this.dragListener = dragListener;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public SheetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_sheet, parent, false);
        return new SheetViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SheetViewHolder holder, int position) {
        CanvasData sheet = sheets.get(position);
        holder.tvSheetName.setText(sheet.getName());

        // Delete callback
        holder.btnDeleteSheet.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onDelete(holder.getAdapterPosition());
            }
        });

        // Drag handle
        holder.dragHandle.setOnTouchListener((v, event) -> {
            if (dragListener != null) {
                dragListener.onStartDrag(holder);
            }
            return false;
        });
        holder.tvSheetName.setOnClickListener(v -> {
            if (sheetNameClickListener != null) {
                sheetNameClickListener.onSheetNameClick(holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return sheets.size();
    }

    public void onItemMove(int fromPosition, int toPosition) {
        if (fromPosition < sheets.size() && toPosition < sheets.size()) {
            Collections.swap(sheets, fromPosition, toPosition);
            notifyItemMoved(fromPosition, toPosition);
        }
    }

    static class SheetViewHolder extends RecyclerView.ViewHolder {
        ImageView dragHandle, canvas;
        TextView tvSheetName;
        ImageButton btnDeleteSheet;

        public SheetViewHolder(@NonNull View itemView) {
            super(itemView);
            dragHandle = itemView.findViewById(R.id.dragHandle);
            canvas = itemView.findViewById(R.id.canvas);
            tvSheetName = itemView.findViewById(R.id.tvSheetName);
            btnDeleteSheet = itemView.findViewById(R.id.btnDeleteSheet);
        }
    }
}
