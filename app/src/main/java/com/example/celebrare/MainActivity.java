package com.example.celebrare;

import com.example.celebrare.TextItemDTO;

import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.AdapterView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.celebrare.databinding.ActivityMainBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {


    private ActivityMainBinding binding;
    private CanvasPagerAdapter canvasPagerAdapter;
    private ViewPager2 viewPager;
    private TextView tvCurrentSheet;


    private int[] fontResIds;
    private List<CanvasData> canvasDataList = new ArrayList<>();

    // Predefined font sizes
    private final String[] fontSizes = {
        "20","24","28","32","36","48","60","72","96","120","144"
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewPager = binding.viewPagerCanvas;

        // ✅ Only 1 default sheet (data-driven)
        canvasDataList.clear();
        long sheetId = System.currentTimeMillis();
        canvasDataList.add(new CanvasData(sheetId, "Sheet 1"));

        // ✅ Initialize adapter with CanvasData list (not fragments)
        canvasPagerAdapter = new CanvasPagerAdapter(this, canvasDataList);
        viewPager.setAdapter(canvasPagerAdapter);
        viewPager.setOffscreenPageLimit(canvasDataList.size());

        setupNavigationButtons();
        setupFontSpinner();
        setupFontSizeSpinner();
        setupButtons();

        // Hide update/delete buttons initially
        binding.btnUpdate.setVisibility(View.GONE);
        binding.btnDel.setVisibility(View.GONE);

        // ✅ Always update selection listener when page changes
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                CanvasData sheet = canvasDataList.get(position);
                binding.tvSheetTitle.setText(sheet.getName());
                CustomCanvasView currentCanvas = getCurrentCanvas();
                if (currentCanvas != null) {
                    setupCanvasSelectionListener(currentCanvas);
                }
                CanvasFragment fragment = canvasPagerAdapter.getFragmentById(canvasDataList.get(position).getId());
                if (fragment != null) {
                    fragment.updateSheetName(canvasDataList.get(position).getName());
                }
            }
        });

        binding.upload.setOnClickListener(v -> {
            int currentPosition = viewPager.getCurrentItem();
            Log.d("UploadDebug", "Current ViewPager position: " + currentPosition);

            CanvasFragment currentFragment = canvasPagerAdapter.getFragmentById(
                canvasPagerAdapter.getItemId(currentPosition)
            );
            if (currentFragment == null) {
                Log.e("UploadDebug", "Current fragment is null!");
                return;
            }

            CanvasData currentSheet = currentFragment.getCanvasData();
            if (currentSheet == null) {
                Log.e("UploadDebug", "Current sheet is null!");
                return;
            }

            Log.d("UploadDebug", "Attempting to upload sheet: " + currentSheet.getName() + " with ID: " + currentSheet.getId());

            new AlertDialog.Builder(this)
                .setTitle("Upload Sheet")
                .setMessage("Really upload sheet \"" + currentSheet.getName() + "\"?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    Log.d("UploadDebug", "Firebase UID: " + uid);

                    FirebaseDatabase.getInstance("https://cauth-d6038-default-rtdb.asia-southeast1.firebasedatabase.app")
                        .getReference("users")
                        .child(uid)
                        .child("sheets")
                        .child(String.valueOf(currentSheet.getId()))
                        .setValue(currentSheet.toMap())
                        .addOnSuccessListener(aVoid -> {
                            Log.d("UploadDebug", "Sheet uploaded successfully: " + currentSheet.getName());
                            Toast.makeText(this, "Uploaded!", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Log.e("UploadDebug", "Upload failed: " + e.getMessage(), e);
                            Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                })
                .setNegativeButton("No", null)
                .show();
        });
        binding.tvloadSheet.setOnClickListener(v -> {
            int currentPosition = viewPager.getCurrentItem();
            CanvasData currentSheet = canvasDataList.get(currentPosition);

            // Convert to DTO
            CanvasDataDTO dto = new CanvasDataDTO(currentSheet);

            Intent intent = new Intent(MainActivity.this, LoadSheetActivity.class);
            intent.putExtra("sheet", dto); // pass DTO, not CanvasData
            startActivityForResult(intent, 200);
        });

        // ✅ Attach listener to the very first canvas
        setupCanvasSelectionListener(getCurrentCanvas());


    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 200 && resultCode == RESULT_OK && data != null) {
            CanvasDataDTO dto = (CanvasDataDTO) data.getSerializableExtra("selectedSheet");
            if (dto != null) {
                // Create a new CanvasData object
                CanvasData newSheet = new CanvasData(dto.id, dto.name);

                List<CustomCanvasView.TextItem> items = new ArrayList<>();
                for (TextItemDTO t : dto.textItems) {
                    // Convert each DTO → actual TextItem
                    items.add(new CustomCanvasView.TextItem(
                        t.text,
                        t.x,
                        t.y,
                        t.fontName,
                        t.size,
                        t.bold,
                        t.italic,
                        t.underline,
                        t.color,
                        Paint.Align.CENTER, // or t.align if you store it
                        null,               // Typeface cannot be serialized; use null
                        t.id
                    ));
                }

                newSheet.setTextItems(items);

                // Add to your list and update UI
                canvasDataList.add(newSheet);
                canvasPagerAdapter.notifyItemInserted(canvasDataList.size() - 1);
                viewPager.setCurrentItem(canvasDataList.size() - 1, true);
            }
        }
    }





    // MainActivity.java
    public void addNewSheet(CanvasData sheetFromFirebase) {
        // 1. Create a deep copy of the sheet to avoid modifying Firebase data
        CanvasData newSheet = new CanvasData(
            System.currentTimeMillis(), // new unique ID for this sheet
            sheetFromFirebase.getName() + " (copy)" // optional: rename to indicate it's loaded
        );

        // Copy text items
        List<CustomCanvasView.TextItem> copiedItems = new ArrayList<>();
        for (CustomCanvasView.TextItem item : sheetFromFirebase.getTextItems()) {
            CustomCanvasView.TextItem newItem = new CustomCanvasView.TextItem(
                item.text,
                item.x,
                item.y,
                item.fontName,
                item.size,
                item.bold,
                item.italic,
                item.underline,
                item.color,
                item.align != null ? item.align : Paint.Align.CENTER,
                item.typeface,
                System.currentTimeMillis() + (long)(Math.random() * 1000) // unique id
            );
            copiedItems.add(newItem);
        }
        newSheet.setTextItems(copiedItems);

        // 2. Add to your canvas adapter or sheet manager
        canvasPagerAdapter.addSheet(newSheet); // You should implement addSheet in your adapter

        // 3. Switch to the new sheet
        int newPosition = canvasPagerAdapter.getItemCount() - 1;
        viewPager.setCurrentItem(newPosition, true);
    }



    /** ==============================
     *  NAVIGATION BETWEEN CANVASES
     *  ============================== */
    private void setupNavigationButtons() {
        binding.btnPrevSlide.setOnClickListener(v -> {
            int prev = viewPager.getCurrentItem() - 1;
            if (prev >= 0) viewPager.setCurrentItem(prev, true);
        });

        binding.btnNextSlide.setOnClickListener(v -> {
            int next = viewPager.getCurrentItem() + 1;
            if (next < canvasPagerAdapter.getItemCount()) {
                viewPager.setCurrentItem(next, true);
            }
        });
    }

    private CustomCanvasView getCurrentCanvas() {
        int position = viewPager.getCurrentItem();
        CanvasData data = canvasDataList.get(position);
        CanvasFragment fragment = canvasPagerAdapter.getFragmentById(data.getId());
        return fragment != null ? fragment.getCustomCanvas() : null;
    }


    /** ==============================
     *  TEXT SELECTION HANDLING
     *  ============================== */
    private void setupCanvasSelectionListener(CustomCanvasView canvas) {
        if (canvas == null) return;

        canvas.setOnTextSelectedListener(item -> {
            if (item != null) {
                binding.btnUpdate.setVisibility(View.VISIBLE);
                binding.btnDel.setVisibility(View.VISIBLE);

                binding.btnBold.setBackgroundColor(item.bold ?
                    ContextCompat.getColor(this, R.color.off_white) :
                    ContextCompat.getColor(this, android.R.color.white));

                binding.btnItalic.setBackgroundColor(item.italic ?
                    ContextCompat.getColor(this, R.color.off_white) :
                    ContextCompat.getColor(this, android.R.color.white));

                binding.btnUnderline.setBackgroundColor(item.underline ?
                    ContextCompat.getColor(this, R.color.off_white) :
                    ContextCompat.getColor(this, android.R.color.white));

                binding.colorPreview.setBackgroundColor(item.color);
                binding.spinnerFontSize.setSelection(getFontSizeIndex((int) item.size));

                int fontIndex = getFontIndex(item.typeface);
                if (fontIndex >= 0) binding.spinnerFonts.setSelection(fontIndex);

            } else {
                // Reset when no text selected
                binding.btnUpdate.setVisibility(View.GONE);
                binding.btnDel.setVisibility(View.GONE);
                binding.btnBold.setBackgroundColor(ContextCompat.getColor(this, android.R.color.white));
                binding.btnItalic.setBackgroundColor(ContextCompat.getColor(this, android.R.color.white));
                binding.btnUnderline.setBackgroundColor(ContextCompat.getColor(this, android.R.color.white));
                binding.colorPreview.setBackgroundColor(0xFF000000);
                binding.spinnerFontSize.setSelection(0);
                binding.spinnerFonts.setSelection(0);
            }
        });
    }

    /** ==============================
     *  FONT & FONT SIZE
     *  ============================== */
    private void setupFontSpinner() {
        String[] fontNames = getResources().getStringArray(R.array.font_names);

        TypedArray fontArray = getResources().obtainTypedArray(R.array.font_res_ids);
        fontResIds = new int[fontArray.length()];
        for (int i = 0; i < fontArray.length(); i++) {
            fontResIds[i] = fontArray.getResourceId(i, -1);
        }
        fontArray.recycle();

        FontSpinnerAdapter adapter = new FontSpinnerAdapter(this, fontNames, fontResIds);
        binding.spinnerFonts.setAdapter(adapter);

        binding.spinnerFonts.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                CustomCanvasView currentCanvas = getCurrentCanvas();
                if (currentCanvas == null) return;

                Typeface typeface = ResourcesCompat.getFont(MainActivity.this, fontResIds[position]);
                CustomCanvasView.TextItem selected = currentCanvas.getSelectedText();

                if (selected != null && typeface != null) {
                    currentCanvas.applyAttributeChange(() -> selected.typeface = typeface);
                } else if (typeface != null) {
                    currentCanvas.setCustomFont(typeface);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupFontSizeSpinner() {
        ArrayAdapter<String> sizeAdapter =
            new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, fontSizes);
        sizeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerFontSize.setAdapter(sizeAdapter);

        binding.spinnerFontSize.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                CustomCanvasView currentCanvas = getCurrentCanvas();
                if (currentCanvas == null) return;

                int size = Integer.parseInt(fontSizes[position]);
                CustomCanvasView.TextItem selected = currentCanvas.getSelectedText();

                if (selected != null) {
                    currentCanvas.applyAttributeChange(() -> selected.size = size);
                } else {
                    currentCanvas.setTextSize(size);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private int getFontSizeIndex(int size) {
        for (int i = 0; i < fontSizes.length; i++) {
            if (Integer.parseInt(fontSizes[i]) == size) return i;
        }
        return 0;
    }

    private int getFontIndex(Typeface typeface) {
        for (int i = 0; i < fontResIds.length; i++) {
            Typeface tf = ResourcesCompat.getFont(this, fontResIds[i]);
            if (tf != null && tf.equals(typeface)) return i;
        }
        return -1;
    }

    /** ==============================
     *  BUTTON HANDLERS
     *  ============================== */
    private void setupButtons() {

        binding.managerSheet.setOnClickListener(v -> {
            // Code to execute when clicked
            showSheetManagerDialog();
        });

        binding.btnAddText.setOnClickListener(v -> {
            CustomCanvasView currentCanvas = getCurrentCanvas();
            if (currentCanvas != null) {
                Log.d("MainActivity", "btnAddText clicked → currentCanvas: " + currentCanvas
                    + ", sheetId=" + currentCanvas.getSheetId()
                    + ", textItems=" + currentCanvas.getTextItems().size());
                showTextInputDialog(currentCanvas);
                currentCanvas.saveStateForUndo();
            }
        });

        binding.btnUpdate.setOnClickListener(v -> {
            CustomCanvasView currentCanvas = getCurrentCanvas();
            if (currentCanvas != null) {
                CustomCanvasView.TextItem selected = currentCanvas.getSelectedText();
                if (selected != null) showUpdateTextDialog(currentCanvas, selected);
            }
        });

        binding.btnDel.setOnClickListener(v -> {
            CustomCanvasView currentCanvas = getCurrentCanvas();
            if (currentCanvas != null) {
                CustomCanvasView.TextItem selected = currentCanvas.getSelectedText();
                if (selected != null) {
                    currentCanvas.saveStateForUndo();
                    currentCanvas.removeText(selected);
                    currentCanvas.invalidate();
                }
            }
        });

        binding.btnBold.setOnClickListener(v -> {
            CustomCanvasView currentCanvas = getCurrentCanvas();
            if (currentCanvas != null) toggleTextStyle(currentCanvas, "bold");
        });

        binding.btnItalic.setOnClickListener(v -> {
            CustomCanvasView currentCanvas = getCurrentCanvas();
            if (currentCanvas != null) toggleTextStyle(currentCanvas, "italic");
        });

        binding.btnUnderline.setOnClickListener(v -> {
            CustomCanvasView currentCanvas = getCurrentCanvas();
            if (currentCanvas != null) toggleTextStyle(currentCanvas, "underline");
        });

        binding.btnTextColor.setOnClickListener(v -> {
            CustomCanvasView currentCanvas = getCurrentCanvas();
            if (currentCanvas != null) showColorPickerDialog(currentCanvas);
        });
        binding.btnUndo.setOnClickListener(v -> {
            CustomCanvasView currentCanvas = getCurrentCanvas();
            if (currentCanvas != null) currentCanvas.undo();
        });

        binding.btnRedo.setOnClickListener(v -> {
            CustomCanvasView currentCanvas = getCurrentCanvas();
            if (currentCanvas != null) currentCanvas.redo();
        });
    }


    private void toggleTextStyle(CustomCanvasView canvas,String style) {
        CustomCanvasView currentCanvas = getCurrentCanvas();
        if (currentCanvas == null) return;

        CustomCanvasView.TextItem selected = currentCanvas.getSelectedText();

        if (selected != null) {
            currentCanvas.applyAttributeChange(() -> {
                switch (style) {
                    case "bold": selected.bold = !selected.bold; break;
                    case "italic": selected.italic = !selected.italic; break;
                    case "underline": selected.underline = !selected.underline; break;
                }
            });

            updateStyleButtons(selected.bold, selected.italic, selected.underline);

        } else {
            // Update "future text" style
            switch (style) {
                case "bold": currentCanvas.setBold(!currentCanvas.isBold()); break;
                case "italic": currentCanvas.setItalic(!currentCanvas.isItalic()); break;
                case "underline": currentCanvas.setUnderline(!currentCanvas.isUnderline()); break;
            }

            // Update button indicators based on future style
            updateStyleButtons(currentCanvas.isBold(), currentCanvas.isItalic(), currentCanvas.isUnderline());
        }
    }

    private void updateStyleButtons(boolean bold, boolean italic, boolean underline) {
        binding.btnBold.setBackgroundColor(bold ?
            ContextCompat.getColor(this, R.color.off_white) :
            ContextCompat.getColor(this, android.R.color.white));

        binding.btnItalic.setBackgroundColor(italic ?
            ContextCompat.getColor(this, R.color.off_white) :
            ContextCompat.getColor(this, android.R.color.white));

        binding.btnUnderline.setBackgroundColor(underline ?
            ContextCompat.getColor(this, R.color.off_white) :
            ContextCompat.getColor(this, android.R.color.white));
    }

    /** ==============================
     *  COLOR PICKER
     *  ============================== */
    private void showColorPickerDialog(CustomCanvasView canvas) {
        View view = getLayoutInflater().inflate(R.layout.dialog_color_picker, null);
        View colorPreview = view.findViewById(R.id.colorPreview);
        SeekBar seekRed = view.findViewById(R.id.seekRed);
        SeekBar seekGreen = view.findViewById(R.id.seekGreen);
        SeekBar seekBlue = view.findViewById(R.id.seekBlue);

        SeekBar.OnSeekBarChangeListener listener = new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int color = Color.rgb(seekRed.getProgress(), seekGreen.getProgress(), seekBlue.getProgress());
                colorPreview.setBackgroundColor(color);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        };

        seekRed.setOnSeekBarChangeListener(listener);
        seekGreen.setOnSeekBarChangeListener(listener);
        seekBlue.setOnSeekBarChangeListener(listener);

        new AlertDialog.Builder(this)
            .setTitle("Pick a Color")
            .setView(view)
            .setPositiveButton("OK", (dialog, which) -> {
                int color = Color.rgb(seekRed.getProgress(), seekGreen.getProgress(), seekBlue.getProgress());
                CustomCanvasView currentCanvas = getCurrentCanvas();
                if (currentCanvas != null) {
                    CustomCanvasView.TextItem selected = currentCanvas.getSelectedText();
                    if (selected != null) {
                        selected.color = color;
                        currentCanvas.invalidate();
                    } else {
                        currentCanvas.setTextColor(color);
                    }
                    binding.colorPreview.setBackgroundColor(color);
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void showTextInputDialog(CustomCanvasView canvas) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter text");
        final EditText input = new EditText(this);
        input.setHint("Type something...");
        builder.setView(input);
        builder.setPositiveButton("OK", (dialog, which) -> {
            String text = input.getText().toString().trim();
            if (!text.isEmpty()) canvas.addText(text);
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showUpdateTextDialog(CustomCanvasView canvas, CustomCanvasView.TextItem selected) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Update text");

        final EditText input = new EditText(this);
        input.setText(selected.text); // pre-fill with current text
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String newText = input.getText().toString().trim();
            if (!newText.isEmpty() && !newText.equals(selected.text)) {
                canvas.applyAttributeChange(() -> selected.text = newText);
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }
    private void showSheetManagerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_manage_sheets, null);
        builder.setView(dialogView);

        RecyclerView recyclerView = dialogView.findViewById(R.id.recyclerSheets);
        Button btnAddSheet = dialogView.findViewById(R.id.btnAddSheet);
        Button btnDone = dialogView.findViewById(R.id.btnDone);

        // Adapter
        final SheetAdapter[] sheetAdapterHolder = new SheetAdapter[1];
        sheetAdapterHolder[0] = new SheetAdapter(
            canvasDataList,
            holder -> { /* drag */ },
            position -> {
                canvasDataList.remove(position);
                sheetAdapterHolder[0].notifyItemRemoved(position);
                canvasPagerAdapter.removeCanvas(position); // ✅ remove via DATA
            }
        );

        SheetAdapter sheetAdapter = sheetAdapterHolder[0];
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(sheetAdapter);

        sheetAdapter.setOnSheetNameClickListener(position -> {
            CanvasData sheet = canvasDataList.get(position);

            AlertDialog.Builder builder0 = new AlertDialog.Builder(this);
            builder0.setTitle("Rename Sheet");

            EditText input = new EditText(this);
            input.setText(sheet.getName());
            builder0.setView(input);

            builder0.setPositiveButton("OK", (dialog, which) -> {
                String newName = input.getText().toString().trim();
                if (!newName.isEmpty()) {
                    sheet.setName(newName);
                    sheetAdapter.notifyItemChanged(position);
                    if (position == viewPager.getCurrentItem()) {
                        binding.tvSheetTitle.setText(newName);
                    }
                    CanvasFragment fragment = canvasPagerAdapter.getFragmentById(sheet.getId());
                    if (fragment != null) {
                        fragment.updateSheetName(newName);
                    }

                }
            });

            builder0.setNegativeButton("Cancel", null);
            builder0.show();

        });


        // Drag & drop
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(
            new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
                @Override
                public boolean onMove(@NonNull RecyclerView recyclerView,
                                      @NonNull RecyclerView.ViewHolder from,
                                      @NonNull RecyclerView.ViewHolder to) {
                    int fromPos = from.getAdapterPosition();
                    int toPos = to.getAdapterPosition();

                    Collections.swap(canvasDataList, fromPos, toPos);
                    sheetAdapter.notifyItemMoved(fromPos, toPos);

                    // Keep ViewPager in sync
                    canvasPagerAdapter.setCanvasList(canvasDataList);
                    return true;
                }

                @Override
                public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {

                }
            });
        itemTouchHelper.attachToRecyclerView(recyclerView);

        AlertDialog dialog = builder.create();

        btnAddSheet.setOnClickListener(v -> {
            long newId = System.currentTimeMillis();
            CanvasData newSheet = new CanvasData(newId, "Sheet " + (canvasDataList.size() + 1));
            canvasPagerAdapter.addCanvas(newSheet);
            canvasPagerAdapter.notifyItemInserted(canvasDataList.size() - 1);
            viewPager.setCurrentItem(canvasDataList.size() - 1, true);
            viewPager.post(() -> {
                CustomCanvasView newCanvas = getCurrentCanvas();
                if (newCanvas != null) {
                    setupCanvasSelectionListener(newCanvas);
                }
                if (sheetAdapter != null) {
                    sheetAdapter.notifyItemInserted(canvasDataList.size() - 1);
                }
            });
        });


        // Done
        btnDone.setOnClickListener(v -> {
            canvasPagerAdapter.setCanvasList(canvasDataList); // ✅ update from DATA
            dialog.dismiss();
        });

        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                (int) (getResources().getDisplayMetrics().widthPixels * 0.9),   // 90% of screen width
                (int) (getResources().getDisplayMetrics().heightPixels * 0.8)   // 80% of screen height
            );
        }
    }
}
