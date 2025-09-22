package com.example.celebrare;

import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.AdapterView;
import android.widget.SeekBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import com.example.celebrare.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private CustomCanvasView customCanvas;
    private int[] fontResIds;
    private final String[] fontSizes = {
        "20","24","28","32","36","48","60","72","96","120","144"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        customCanvas = binding.customCanvas;

        setupFontSpinner();
        setupFontSizeSpinner();
        setupButtons();
        binding.btnUpdate.setVisibility(View.GONE);
        binding.btnDel.setVisibility(View.GONE);

        customCanvas.setOnTextSelectedListener(item -> {

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

                binding.spinnerFontSize.setSelection(getFontSizeIndex((int)item.size));

                int fontIndex = getFontIndex(item.typeface);
                if (fontIndex >= 0) binding.spinnerFonts.setSelection(fontIndex);

            } else {

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

    private void setupFontSpinner() {
        String[] fontNames = getResources().getStringArray(R.array.font_names);

        TypedArray fontArray = getResources().obtainTypedArray(R.array.font_res_ids);
        fontResIds = new int[fontArray.length()];
        for (int i = 0; i < fontArray.length(); i++) fontResIds[i] = fontArray.getResourceId(i, -1);
        fontArray.recycle();

        FontSpinnerAdapter adapter = new FontSpinnerAdapter(this, fontNames, fontResIds);
        binding.spinnerFonts.setAdapter(adapter);

        binding.spinnerFonts.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Typeface typeface = ResourcesCompat.getFont(MainActivity.this, fontResIds[position]);
                CustomCanvasView.TextItem selected = customCanvas.getSelectedText();
                if (selected != null && typeface != null) {
                    customCanvas.applyAttributeChange(() -> selected.typeface = typeface);
                } else if (typeface != null) {
                    customCanvas.setCustomFont(typeface);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

    }


    private void setupFontSizeSpinner() {
        ArrayAdapter<String> sizeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, fontSizes);
        sizeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerFontSize.setAdapter(sizeAdapter);

        binding.spinnerFontSize.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int size = Integer.parseInt(fontSizes[position]);
                CustomCanvasView.TextItem selected = customCanvas.getSelectedText();
                if (selected != null) {
                    customCanvas.applyAttributeChange(() -> selected.size = size);
                } else {
                    customCanvas.setTextSize(size);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

    }

    private void setupButtons() {

        binding.btnAddText.setOnClickListener(v -> {
            showTextInputDialog();
            customCanvas.saveStateForUndo();
        });

        binding.btnUpdate.setEnabled(true);

        binding.btnUpdate.setOnClickListener(v -> {
            CustomCanvasView.TextItem selected = customCanvas.getSelectedText();
            if (selected != null) {
                showUpdateTextDialog(selected);
            }
        });

        binding.btnDel.setOnClickListener(v -> {
            CustomCanvasView.TextItem selected = customCanvas.getSelectedText();
            if (selected != null) {

                customCanvas.saveStateForUndo();
                customCanvas.removeText(selected);
                customCanvas.invalidate();
            }
        });

        binding.btnBold.setOnClickListener(v -> {
            CustomCanvasView.TextItem selected = customCanvas.getSelectedText();

            if (selected != null) {
                customCanvas.applyAttributeChange(() -> {
                    selected.bold = !selected.bold;
                });

                v.setBackgroundColor(selected.bold
                    ? ContextCompat.getColor(this, R.color.off_white)
                    : ContextCompat.getColor(this, android.R.color.white));

            } else {
                boolean newState = !customCanvas.isBold();
                customCanvas.setBold(newState);
                v.setBackgroundColor(newState
                    ? ContextCompat.getColor(this, R.color.off_white)
                    : ContextCompat.getColor(this, android.R.color.white));
            }
        });


        binding.btnItalic.setOnClickListener(v -> {
            CustomCanvasView.TextItem selected = customCanvas.getSelectedText();
            if (selected != null) {
                customCanvas.applyAttributeChange(() -> selected.italic = !selected.italic);

                v.setBackgroundColor(selected.italic
                    ? ContextCompat.getColor(this, R.color.off_white)
                    : ContextCompat.getColor(this, android.R.color.white));
            } else {
                boolean newState = !customCanvas.isItalic();
                customCanvas.setItalic(newState);
                v.setBackgroundColor(newState
                    ? ContextCompat.getColor(this, R.color.off_white)
                    : ContextCompat.getColor(this, android.R.color.white));
            }
        });

        binding.btnUnderline.setOnClickListener(v -> {
            CustomCanvasView.TextItem selected = customCanvas.getSelectedText();
            if (selected != null) {
                customCanvas.applyAttributeChange(() -> selected.underline = !selected.underline);

                v.setBackgroundColor(selected.underline
                    ? ContextCompat.getColor(this, R.color.off_white)
                    : ContextCompat.getColor(this, android.R.color.white));
            } else {
                boolean newState = !customCanvas.isUnderline();
                customCanvas.setUnderline(newState);
                v.setBackgroundColor(newState
                    ? ContextCompat.getColor(this, R.color.off_white)
                    : ContextCompat.getColor(this, android.R.color.white));
            }
        });

        binding.btnTextColor.setOnClickListener(v -> showColorPickerDialog());

        binding.btnUndo.setOnClickListener(v -> customCanvas.undo());

        binding.btnRedo.setOnClickListener(v -> customCanvas.redo());
    }

    private void showColorPickerDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_color_picker, null);
        View colorPreview = view.findViewById(R.id.colorPreview);
        SeekBar seekRed = view.findViewById(R.id.seekRed);
        SeekBar seekGreen = view.findViewById(R.id.seekGreen);
        SeekBar seekBlue = view.findViewById(R.id.seekBlue);

        int[] rgb = {0, 0, 0};

        SeekBar.OnSeekBarChangeListener listener = new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                rgb[0] = seekRed.getProgress();
                rgb[1] = seekGreen.getProgress();
                rgb[2] = seekBlue.getProgress();
                int color = Color.rgb(rgb[0], rgb[1], rgb[2]);
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
                CustomCanvasView.TextItem selected = customCanvas.getSelectedText();
                if (selected != null) {
                    selected.color = color;
                    customCanvas.invalidate();
                } else {
                    customCanvas.setTextColor(color);
                }
                binding.colorPreview.setBackgroundColor(color);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void showTextInputDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter text");
        final EditText input = new EditText(this);
        input.setHint("Type something...");
        builder.setView(input);
        builder.setPositiveButton("OK", (dialog, which) -> {
            String text = input.getText().toString().trim();
            if (!text.isEmpty()) customCanvas.addText(text);
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }
    private void showUpdateTextDialog(CustomCanvasView.TextItem selected) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Update text");

        final EditText input = new EditText(this);
        input.setText(selected.text); // pre-fill with current text
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String newText = input.getText().toString().trim();
            if (!newText.isEmpty() && !newText.equals(selected.text)) {
                customCanvas.applyAttributeChange(() -> selected.text = newText);
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

}
