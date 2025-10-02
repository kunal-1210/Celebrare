package com.example.celebrare;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.FirebaseDatabase;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

public class LoadSheetAdapter extends RecyclerView.Adapter<LoadSheetAdapter.SheetViewHolder> {

    private List<CanvasData> sheetList;
    private OnSheetClickListener listener;

    public interface OnSheetClickListener {
        void onSheetClick(CanvasData sheet);
    }

    public LoadSheetAdapter(List<CanvasData> sheetList, OnSheetClickListener listener) {
        this.sheetList = sheetList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SheetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_load_sheet, parent, false);
        return new SheetViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SheetViewHolder holder, int position) {
        CanvasData sheet = sheetList.get(position);
        holder.sheetName.setText(sheet.getName());
        holder.sheetPreview.setSheetData(sheet);


        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onSheetClick(sheet);
        });

        holder.btnDelete.setOnClickListener(v -> {
            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            FirebaseDatabase.getInstance("https://cauth-d6038-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("users")
                .child(uid)
                .child("sheets")
                .child(String.valueOf(sheet.getId()))
                .removeValue()
                .addOnSuccessListener(aVoid -> {
                    sheetList.remove(position);
                    notifyItemRemoved(position);
                })
                .addOnFailureListener(e ->
                    Toast.makeText(holder.itemView.getContext(),
                        "Failed to delete sheet: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show());
        });
    }

    @Override
    public int getItemCount() {
        return sheetList.size();
    }

    static class SheetViewHolder extends RecyclerView.ViewHolder {
        Button btnDelete;
        MiniCanvasView sheetPreview;
        TextView sheetName;

        public SheetViewHolder(@NonNull View itemView) {
            super(itemView);
            sheetPreview = itemView.findViewById(R.id.sheetPreview);
            sheetName = itemView.findViewById(R.id.sheetName);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
