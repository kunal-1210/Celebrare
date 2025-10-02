package com.example.celebrare;

import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class LoadSheetActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private LoadSheetAdapter adapter;
    private List<CanvasData> sheetList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_load_sheet);

        recyclerView = findViewById(R.id.sheetRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new LoadSheetAdapter(sheetList, sheet -> {
            // Convert CanvasData to DTO before returning
            Intent resultIntent = new Intent();
            CanvasDataDTO dto = new CanvasDataDTO(sheet); // convert CanvasData → DTO
            resultIntent.putExtra("selectedSheet", dto);   // ✅ Send DTO instead
            setResult(RESULT_OK, resultIntent);

            finish();
        });

        recyclerView.setAdapter(adapter);

        loadSheetsFromFirebase();
    }

    private void loadSheetsFromFirebase() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseDatabase.getInstance("https://<YOUR_PROJECT_URL>.firebaseio.com/\"")
            .getReference("users")
            .child(uid)
            .child("sheets")
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    sheetList.clear();

                    for (DataSnapshot sheetSnap : snapshot.getChildren()) {
                        long sheetId = Long.parseLong(sheetSnap.getKey());
                        String sheetName = sheetSnap.child("name").getValue(String.class);
                        if (sheetName == null) sheetName = "Untitled Sheet";

                        CanvasData sheet = new CanvasData(sheetId, sheetName);

                        List<CustomCanvasView.TextItem> items = new ArrayList<>();
                        DataSnapshot textItemsSnap = sheetSnap.child("textItems");
                        for (DataSnapshot textSnap : textItemsSnap.getChildren()) {
                            long id = Long.parseLong(textSnap.getKey());

                            String text = textSnap.child("content").getValue(String.class);
                            if (text == null) text = "";

                            float x = textSnap.child("x").getValue(Float.class) != null ? textSnap.child("x").getValue(Float.class) : 0f;
                            float y = textSnap.child("y").getValue(Float.class) != null ? textSnap.child("y").getValue(Float.class) : 0f;
                            float size = textSnap.child("size").getValue(Float.class) != null ? textSnap.child("size").getValue(Float.class) : 16f;

                            boolean bold = textSnap.child("bold").getValue(Boolean.class) != null ? textSnap.child("bold").getValue(Boolean.class) : false;
                            boolean italic = textSnap.child("italic").getValue(Boolean.class) != null ? textSnap.child("italic").getValue(Boolean.class) : false;
                            boolean underline = textSnap.child("underline").getValue(Boolean.class) != null ? textSnap.child("underline").getValue(Boolean.class) : false;

                            int color = textSnap.child("color").getValue(Integer.class) != null ? textSnap.child("color").getValue(Integer.class) : 0xFF000000;
                            String fontName = textSnap.child("fontName").getValue(String.class);
                            if (fontName == null) fontName = "default";

                            items.add(new CustomCanvasView.TextItem(
                                text, x, y, fontName, size, bold, italic, underline,
                                color, Paint.Align.CENTER, null, id
                            ));
                        }

                        sheet.setTextItems(items);
                        sheetList.add(sheet);
                    }

                    adapter.notifyDataSetChanged();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(LoadSheetActivity.this, "Failed to load sheets: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
    }
}
