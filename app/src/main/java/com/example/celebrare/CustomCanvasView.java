package com.example.celebrare;

import android.graphics.Typeface;
import android.text.TextPaint;
import android.graphics.Paint;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

public class CustomCanvasView extends View {

    private List<TextItem> textItems = new ArrayList<>();
    private TextPaint textPaint;

    private Typeface currentTypeface = Typeface.DEFAULT;

    // Font style
    private boolean isBold = false;
    private boolean isItalic = false;
    private boolean isUnderline = false;
    private float textSize = 50f;
    private String fontName = "sans-serif";
    private int textColor = 0xFF000000; // default black
    private Paint.Align textAlign = Paint.Align.CENTER;
    private TextItem selectedText = null;
    private float offsetX, offsetY;
    private List<List<TextItem>> undoStack = new ArrayList<>();
    private List<List<TextItem>> redoStack = new ArrayList<>();


    public CustomCanvasView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(textColor);
        textPaint.setTextSize(textSize);
        textPaint.setTextAlign(textAlign);
        saveStateForUndo();
    }

    public void addText(String text) {
        saveStateForUndo();
        TextItem item = new TextItem(text, getWidth()/2f, getHeight()/2f, fontName, textSize, isBold, isItalic, isUnderline, textColor, textAlign,currentTypeface);
        textItems.add(item);
        saveStateForUndo();
        invalidate(); // redraw canvas
    }

    public boolean isBold() { return isBold; }
    public boolean isItalic() { return isItalic; }
    public boolean isUnderline() { return isUnderline; }

    public void setTextSize(float size) { textSize = size; }
    public void setBold(boolean bold) { isBold = bold; }
    public void setItalic(boolean italic) { isItalic = italic; }
    public void setUnderline(boolean underline) { isUnderline = underline; }
    public void setTextFont(String font) { fontName = font; }
    public void setTextColor(int color) { textColor = color; invalidate(); }


    private boolean isMoving = false;
    private float startX, startY;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (textItems.isEmpty()) {
            Log.d("TouchDebug", "No text items available.");
            return false;
        }
        float x = event.getX();
        float y = event.getY();
        Log.d("TouchDebug", "Event action: " + event.getAction() + " | X: " + x + " | Y: " + y);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                selectedText = findTextAtPosition(x, y);
                Log.d("TouchDebug", "ACTION_DOWN → Selected text: " +
                    (selectedText != null ? selectedText.text : "null"));
                if (textSelectedListener != null) {
                    textSelectedListener.onTextSelected(selectedText);
                    Log.d("TouchDebug", "Notified listener about selection: " +
                        (selectedText != null ? selectedText.text : "null"));
                }

                if (selectedText != null) {
                    offsetX = x - selectedText.x;
                    offsetY = y - selectedText.y;
                    Log.d("TouchDebug", "Touch offset set → offsetX: " + offsetX + " offsetY: " + offsetY);

                } else {
                    // Tapped empty space → deselect
                    Log.d("TouchDebug", "Tapped empty space. Clearing selection.");
                    selectedText = null;
                    invalidate();
                }
                break;


            case MotionEvent.ACTION_MOVE:
                if (selectedText != null) {
                    float newX = clampX(x - offsetX, textPaint.measureText(selectedText.text));
                    float newY = clampY(y - offsetY, selectedText.size);

                    if (!isMoving && (Math.abs(newX - startX) > 5 || Math.abs(newY - startY) > 5)) {
                        isMoving = true;
                    }
                    selectedText.x = newX;
                    selectedText.y = newY;
                    invalidate();
                }
                break;

            case MotionEvent.ACTION_UP:
                if (isMoving) {
                    saveStateForUndo();
                }
                isMoving = false;
                break;
        }

        return true;
    }
    public void applyAttributeChange(Runnable change) {

        saveStateForUndo();
        change.run();
        invalidate();
    }


    public interface OnTextSelectedListener {
        void onTextSelected(TextItem item);
    }

    private OnTextSelectedListener textSelectedListener;

    public void setOnTextSelectedListener(OnTextSelectedListener listener) {
        this.textSelectedListener = listener;
    }

    public TextItem getSelectedText() {
        Log.d("SelectionDebug", "getSelectedText() called. Currently selected: " + (selectedText != null ? selectedText.text : "null"));
        return selectedText;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        for (TextItem item : textItems) {
            textPaint.setTextSize(item.size);
            textPaint.setColor(item.color);
            textPaint.setTextAlign(item.align);

            int style;
            if (item.bold && item.italic) {
                style = Typeface.BOLD_ITALIC;
            } else if (item.bold) {
                style = Typeface.BOLD;
            } else if (item.italic) {
                style = Typeface.ITALIC;
            } else {
                style = Typeface.NORMAL;
            }

            Log.d("FontDebug", "Drawing text: '" + item.text + "' typeface=" + item.typeface);
            Typeface baseTypeface = (item.typeface != null) ? item.typeface : Typeface.create(item.fontName, Typeface.NORMAL);
            int styleFlags = Typeface.NORMAL;
            if (item.bold && item.italic) styleFlags = Typeface.BOLD_ITALIC;
            else if (item.bold) styleFlags = Typeface.BOLD;
            else if (item.italic) styleFlags = Typeface.ITALIC;

            textPaint.setTypeface(Typeface.create(baseTypeface, styleFlags));

            textPaint.setUnderlineText(item.underline);

            float textWidth = textPaint.measureText(item.text);
            Paint.FontMetrics metrics = textPaint.getFontMetrics();
            float textHeight = metrics.bottom - metrics.top;


            canvas.drawText(item.text, item.x, item.y, textPaint);


            if (item == selectedText) {
                Paint borderPaint = new Paint();
                borderPaint.setStyle(Paint.Style.STROKE);
                borderPaint.setColor(0xFF00FF00); // green border
                borderPaint.setStrokeWidth(3);

                float left = item.x - textWidth / 2;
                float top = item.y + metrics.top;
                float right = item.x + textWidth / 2;
                float bottom = item.y + metrics.bottom;

                canvas.drawRect(left, top, right, bottom, borderPaint);
            }
        }
    }

    private TextItem findTextAtPosition(float x, float y) {
        for (int i = textItems.size() - 1; i >= 0; i--) { // check topmost first
            TextItem item = textItems.get(i);
            float textWidth = textPaint.measureText(item.text);
            float textHeight = item.size;
            float left = item.x - textWidth / 2;
            float right = item.x + textWidth / 2;
            float top = item.y - textHeight;
            float bottom = item.y;

            if (x >= left && x <= right && y >= top && y <= bottom) {
                return item;
            }
        }
        return null;
    }

    private float clampX(float x, float textWidth) {
        float minX = textWidth / 2;
        float maxX = getWidth() - textWidth / 2;
        return Math.max(minX, Math.min(x, maxX));
    }

    private float clampY(float y, float textHeight) {
        float minY = textHeight;
        float maxY = getHeight();
        return Math.max(minY, Math.min(y, maxY));
    }

    public void setCustomFont(Typeface typeface) {
        if (textPaint != null) {
            textPaint.setTypeface(typeface);
            currentTypeface = typeface;
            invalidate(); // Redraw canvas
        }
    }

    public static class TextItem {
        public String text, fontName;
        public float x, y, size;
        public boolean bold, italic, underline;
        public int color;
        public Paint.Align align;
        public Typeface typeface;

        TextItem(String text, float x, float y, String fontName, float size, boolean bold, boolean italic, boolean underline, int color, Paint.Align align,Typeface typeface) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.fontName = fontName;
            this.size = size;
            this.bold = bold;
            this.italic = italic;
            this.underline = underline;
            this.color = color;
            this.align = align;
            this.typeface = typeface;
        }
    }

    private List<TextItem> deepCopyTextItems(List<TextItem> original) {
        List<TextItem> copy = new ArrayList<>();
        for (TextItem item : original) {
            copy.add(new TextItem(
                item.text,
                item.x,
                item.y,
                item.fontName,
                item.size,
                item.bold,
                item.italic,
                item.underline,
                item.color,
                item.align,
                item.typeface
            ));
        }
        return copy;
    }
    public void saveStateForUndo() {

        if (undoStack.isEmpty()) {
            undoStack.add(deepCopyTextItems(textItems));
            return;
        }

        // Only save if there is a REAL change
        if (!textItemsEqual(undoStack.get(undoStack.size() - 1), textItems)) {
            undoStack.add(deepCopyTextItems(textItems));
            redoStack.clear();
            Log.d("UndoRedoDebug", "New state saved. Undo stack size: " + undoStack.size());
        } else {
            Log.d("UndoRedoDebug", "Skipped saving snapshot - No real change detected");
        }
    }

    // Compare two lists of TextItem objects
    private boolean textItemsEqual(List<TextItem> a, List<TextItem> b) {
        if (a.size() != b.size()) return false;

        for (int i = 0; i < a.size(); i++) {
            TextItem t1 = a.get(i), t2 = b.get(i);

            if (!t1.text.equals(t2.text)
                || Math.round(t1.x * 10) != Math.round(t2.x * 10)
                || Math.round(t1.y * 10) != Math.round(t2.y * 10)
                || t1.size != t2.size
                || t1.bold != t2.bold
                || t1.italic != t2.italic
                || t1.underline != t2.underline
                || t1.color != t2.color
                || t1.typeface != t2.typeface) {
                return false;
            }
        }
        return true;
    }

    public void undo() {

        if (undoStack.isEmpty()) {
            Log.d("UndoRedoDebug", "Undo stack is EMPTY - nothing to undo");
            return;
        }

        redoStack.add(deepCopyTextItems(textItems));

        List<TextItem> lastState = undoStack.remove(undoStack.size() - 1);

        textItems.clear();
        textItems.addAll(lastState);

        selectedText = null;
        invalidate();
    }
    public void redo() {
        if (!redoStack.isEmpty()) {
            undoStack.add(deepCopyTextItems(textItems));
            textItems = redoStack.remove(redoStack.size() - 1);
            selectedText = null;
            invalidate();
        }
    }
    public void removeText(TextItem textItem) {
        if (textItem != null) {
            saveStateForUndo();
            textItems.remove(textItem);
            if (selectedText == textItem) selectedText = null;
            invalidate();
        }
    }

}
