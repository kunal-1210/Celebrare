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
import androidx.viewpager2.widget.ViewPager2;

import com.example.celebrare.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private CanvasPagerAdapter canvasPagerAdapter;
    private ViewPager2 viewPager;

    private int[] fontResIds;

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

        // Create multiple canvases
        List<CanvasFragment> canvasFragments = new ArrayList<>();
        for (int i = 0; i < 3; i++) { // You can dynamically change count
            canvasFragments.add(CanvasFragment.newInstance(i));
        }

        canvasPagerAdapter = new CanvasPagerAdapter(this, canvasFragments);
        viewPager.setAdapter(canvasPagerAdapter);

        setupNavigationButtons();
        setupFontSpinner();
        setupFontSizeSpinner();
        setupButtons();

        // Initially hide update/delete buttons
        binding.btnUpdate.setVisibility(View.GONE);
        binding.btnDel.setVisibility(View.GONE);

        // Listen to currently selected fragment's canvas
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                CustomCanvasView currentCanvas = getCurrentCanvas();
                if (currentCanvas != null) {
                    setupCanvasSelectionListener(currentCanvas);
                }
            }
        });

        // Setup listener for the very first canvas
        setupCanvasSelectionListener(getCurrentCanvas());
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
        CanvasFragment currentFragment = canvasPagerAdapter.getFragmentAt(viewPager.getCurrentItem());
        return currentFragment != null ? currentFragment.getCustomCanvas() : null;
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
        binding.btnAddText.setOnClickListener(v -> {
            CustomCanvasView currentCanvas = getCurrentCanvas();
            if (currentCanvas != null) {
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

        binding.btnBold.setOnClickListener(v -> toggleTextStyle("bold"));
        binding.btnItalic.setOnClickListener(v -> toggleTextStyle("italic"));
        binding.btnUnderline.setOnClickListener(v -> toggleTextStyle("underline"));

        binding.btnTextColor.setOnClickListener(v -> showColorPickerDialog());

        binding.btnUndo.setOnClickListener(v -> {
            CustomCanvasView currentCanvas = getCurrentCanvas();
            if (currentCanvas != null) currentCanvas.undo();
        });

        binding.btnRedo.setOnClickListener(v -> {
            CustomCanvasView currentCanvas = getCurrentCanvas();
            if (currentCanvas != null) currentCanvas.redo();
        });
    }

    private void toggleTextStyle(String style) {
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
        } else {
            switch (style) {
                case "bold": currentCanvas.setBold(!currentCanvas.isBold()); break;
                case "italic": currentCanvas.setItalic(!currentCanvas.isItalic()); break;
                case "underline": currentCanvas.setUnderline(!currentCanvas.isUnderline()); break;
            }
        }
    }

    /** ==============================
     *  COLOR PICKER
     *  ============================== */
    private void showColorPickerDialog() {
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
}
