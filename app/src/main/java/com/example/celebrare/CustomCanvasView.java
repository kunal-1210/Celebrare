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
    private long sheetId;

    private Typeface currentTypeface = Typeface.DEFAULT;

    // Font style
    private boolean isBold = false;
    private boolean isItalic = false;
    private boolean isUnderline = false;
    private float textSize = 20f;
    private String fontName = "sans-serif";
    private int textColor = 0xFF000000; // default black
    private Paint.Align textAlign = Paint.Align.CENTER;
    private TextItem selectedText = null;
    private float offsetX, offsetY;
    private List<List<TextItem>> undoStack = new ArrayList<>();
    private List<List<TextItem>> redoStack = new ArrayList<>();
    private int lastWidth = -1;
    private int lastHeight = -1;

    public long getSheetId() {
        return sheetId;
    }

    public List<TextItem> getTextItems() {
        return textItems;
    }

    public void setTextItems(List<TextItem> items) {
        this.textItems = items;
        invalidate();
    }
    public void setSheetId(long id) {
        this.sheetId = id;
    }
    public void initForSheet(int id) {
        this.sheetId = id;
        textItems.clear(); // new sheet starts blank
        invalidate();
    }
    public CustomCanvasView(Context context) {
        super(context);
        init();
    }

    public CustomCanvasView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CustomCanvasView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(textColor);
        textPaint.setTextSize(textSize);
        textPaint.setTextAlign(textAlign);
        saveStateForUndo();
    }
    private List<String> wrapText(String text, Paint paint, float maxWidth) {
        List<String> lines = new ArrayList<>();

        // Split by existing line breaks first
        String[] paragraphs = text.split("\n");

        for (String paragraph : paragraphs) {
            if (paragraph.trim().isEmpty()) {
                lines.add("");
                continue;
            }

            String[] words = paragraph.split(" ");
            StringBuilder currentLine = new StringBuilder();

            for (String word : words) {
                String testLine = currentLine.length() == 0 ?
                    word : currentLine + " " + word;

                float testWidth = paint.measureText(testLine);

                if (testWidth > maxWidth && currentLine.length() > 0) {
                    // Current line is full, save it and start new line
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                } else {
                    currentLine = new StringBuilder(testLine);
                }
            }

            // Add the last line
            if (currentLine.length() > 0) {
                lines.add(currentLine.toString());
            }
        }

        return lines;
    }

    public void addText(String text) {
        saveStateForUndo();
        long textId = System.currentTimeMillis();
        TextItem item = new TextItem(text, getWidth()/2f, getHeight()/2f, fontName,
            textSize, isBold, isItalic, isUnderline, textColor,
            textAlign, currentTypeface,textId);
        textItems.add(item);
        Log.d("CustomCanvasView", "addText: \"" + text + "\" added with id=" + textId);
        saveStateForUndo();
        invalidate();
    }

    public boolean isBold() { return isBold; }
    public boolean isItalic() { return isItalic; }
    public boolean isUnderline() { return isUnderline; }

    public void setTextSize(float size) { textSize = size; }
    public void setBold(boolean bold) { isBold = bold; }
    public void setItalic(boolean italic) { isItalic = italic; }
    public void setUnderline(boolean underline) { isUnderline = underline; }
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
                    getParent().requestDisallowInterceptTouchEvent(true);
                    offsetX = x - selectedText.x;
                    offsetY = y - selectedText.y;
                    startX = selectedText.x;
                    startY = selectedText.y;
                    isMoving = false;
                    Log.d("TouchDebug", "Touch offset set → offsetX: " + offsetX + " offsetY: " + offsetY);
                } else {
                    Log.d("TouchDebug", "Tapped empty space. Clearing selection.");
                    selectedText = null;
                    invalidate();
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (selectedText != null) {
                    float newX = x - offsetX;
                    float newY = y - offsetY;

                    textPaint.setTextSize(spToPx(selectedText.size));
                    Typeface baseTypeface = (selectedText.typeface != null) ?
                        selectedText.typeface : Typeface.create(selectedText.fontName, Typeface.NORMAL);
                    int styleFlags = Typeface.NORMAL;
                    if (selectedText.bold && selectedText.italic) styleFlags = Typeface.BOLD_ITALIC;
                    else if (selectedText.bold) styleFlags = Typeface.BOLD;
                    else if (selectedText.italic) styleFlags = Typeface.ITALIC;
                    textPaint.setTypeface(Typeface.create(baseTypeface, styleFlags));

                    float maxWidth = getWidth() * 0.8f;

                    // Use cache during dragging too!
                    List<String> lines;
                    if (selectedText.isCacheValid(maxWidth)) {
                        lines = selectedText.cachedLines;
                    } else {
                        lines = wrapText(selectedText.text, textPaint, maxWidth);
                        selectedText.updateCache(lines, maxWidth);
                    }

                    float maxLineWidth = 0;
                    for (String line : lines) {
                        float lineWidth = textPaint.measureText(line);
                        if (lineWidth > maxLineWidth) {
                            maxLineWidth = lineWidth;
                        }
                    }

                    Paint.FontMetrics metrics = textPaint.getFontMetrics();
                    float lineHeight = metrics.bottom - metrics.top + metrics.leading;
                    float totalHeight = lines.size() * lineHeight;

                    // Clamp positions
                    float halfWidth = maxLineWidth / 2;
                    float minX = halfWidth;
                    float maxX = getWidth() - halfWidth;
                    newX = Math.max(minX, Math.min(newX, maxX));

                    float minY = Math.abs(metrics.top);
                    float maxY = getHeight() - totalHeight + lineHeight - Math.abs(metrics.bottom);
                    newY = Math.max(minY, Math.min(newY, maxY));

                    // NOTE: Position changes don't invalidate cache!
                    selectedText.x = newX;
                    selectedText.y = newY;

                    if (!isMoving && (Math.abs(newX - startX) > 5 || Math.abs(newY - startY) > 5)) {
                        isMoving = true;
                    }
                    invalidate();
                }
                break;
            case MotionEvent.ACTION_UP:
                getParent().requestDisallowInterceptTouchEvent(false);
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
    if (selectedText != null) {
        selectedText.invalidateCache(); // Clear cache before change
    }
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
            textPaint.setTextSize(spToPx(item.size));
            textPaint.setColor(item.color);
            textPaint.setTextAlign(item.align);

            Typeface baseTypeface = (item.typeface != null) ?
                item.typeface : Typeface.create(item.fontName, Typeface.NORMAL);
            int styleFlags = Typeface.NORMAL;
            if (item.bold && item.italic) styleFlags = Typeface.BOLD_ITALIC;
            else if (item.bold) styleFlags = Typeface.BOLD;
            else if (item.italic) styleFlags = Typeface.ITALIC;

            textPaint.setTypeface(Typeface.create(baseTypeface, styleFlags));
            textPaint.setUnderlineText(item.underline);

            Paint.FontMetrics metrics = textPaint.getFontMetrics();
            float lineHeight = metrics.bottom - metrics.top + metrics.leading;
            float maxWidth = getWidth() * 0.8f;

            // Use comprehensive cache validation
            List<String> lines;
            if (item.isCacheValid(maxWidth)) {
                lines = item.cachedLines;
                Log.d("CacheDebug", "✓ Cache HIT for: " + item.text);
            } else {
                lines = wrapText(item.text, textPaint, maxWidth);
                item.updateCache(lines, maxWidth);
                Log.d("CacheDebug", "✗ Cache MISS for: " + item.text + " - recalculating");
            }

            float totalHeight = lines.size() * lineHeight;
            float currentY = item.y;
            float maxLineWidth = 0;

            for (String line : lines) {
                float lineWidth = textPaint.measureText(line);
                maxLineWidth = Math.max(maxLineWidth, lineWidth);
                canvas.drawText(line, item.x, currentY, textPaint);
                currentY += lineHeight;
            }

            if (item == selectedText) {
                Paint borderPaint = new Paint();
                borderPaint.setStyle(Paint.Style.STROKE);
                borderPaint.setColor(0xFF00FF00);
                borderPaint.setStrokeWidth(3);

                float left = item.x - maxLineWidth / 2;
                float top = item.y + metrics.top;
                float right = item.x + maxLineWidth / 2;
                float bottom = item.y + totalHeight + metrics.bottom - lineHeight;

                canvas.drawRect(left, top, right, bottom, borderPaint);
            }
        }
    }
    private TextItem findTextAtPosition(float x, float y) {
        for (int i = textItems.size() - 1; i >= 0; i--) {
            TextItem item = textItems.get(i);

            textPaint.setTextSize(spToPx(item.size));
            Typeface baseTypeface = (item.typeface != null) ?
                item.typeface : Typeface.create(item.fontName, Typeface.NORMAL);
            int styleFlags = Typeface.NORMAL;
            if (item.bold && item.italic) styleFlags = Typeface.BOLD_ITALIC;
            else if (item.bold) styleFlags = Typeface.BOLD;
            else if (item.italic) styleFlags = Typeface.ITALIC;
            textPaint.setTypeface(Typeface.create(baseTypeface, styleFlags));

            Paint.FontMetrics metrics = textPaint.getFontMetrics();
            float lineHeight = metrics.bottom - metrics.top + metrics.leading;
            float maxWidth = getWidth() * 0.8f;

            // Use cache in hit-testing too!
            List<String> lines;
            if (item.isCacheValid(maxWidth)) {
                lines = item.cachedLines;
            } else {
                lines = wrapText(item.text, textPaint, maxWidth);
                item.updateCache(lines, maxWidth);
            }

            float maxLineWidth = 0;
            for (String line : lines) {
                maxLineWidth = Math.max(maxLineWidth, textPaint.measureText(line));
            }

            float totalHeight = lines.size() * lineHeight;
            float left = item.x - maxLineWidth / 2;
            float right = item.x + maxLineWidth / 2;
            float top = item.y + metrics.top;
            float bottom = item.y + totalHeight + metrics.bottom - lineHeight;

            if (x >= left && x <= right && y >= top && y <= bottom) {
                return item;
            }
        }
        return null;
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
        public long id;

        public List<String> cachedLines = null;
        public float cachedMaxWidth = -1;
        public float cachedSize = -1;
        public String cachedText = null;
        public boolean cachedBold = false;
        public boolean cachedItalic = false;
        public Typeface cachedTypeface = null;
        public Paint.Align cachedAlign = null;


        TextItem(String text, float x, float y, String fontName, float size, boolean bold, boolean italic, boolean underline, int color, Paint.Align align,Typeface typeface,long id) {
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
            this.id = id;
        }
        public void invalidateCache() {
            cachedLines = null;
            cachedMaxWidth = -1;
            cachedSize = -1;
            cachedText = null;
            cachedBold = false;
            cachedItalic = false;
            cachedTypeface = null;
            cachedAlign = null;
        }
        public boolean isCacheValid(float maxWidth) {
            return cachedLines != null
                && cachedMaxWidth == maxWidth
                && cachedSize == size
                && text.equals(cachedText)
                && cachedBold == bold
                && cachedItalic == italic
                && cachedTypeface == typeface
                && cachedAlign == align;
        }

        // Update cache with current properties
        public void updateCache(List<String> lines, float maxWidth) {
            cachedLines = lines;
            cachedMaxWidth = maxWidth;
            cachedSize = size;
            cachedText = text;
            cachedBold = bold;
            cachedItalic = italic;
            cachedTypeface = typeface;
            cachedAlign = align;
        }
    }

    private List<TextItem> deepCopyTextItems(List<TextItem> original) {
        List<TextItem> copy = new ArrayList<>();
        for (TextItem item : original) {
            TextItem newItem = new TextItem(
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
                item.typeface,
                item.id
            );
            // Don't copy cache - force fresh calculation
            newItem.cachedLines = null;
            copy.add(newItem);
        }
        return copy;
    }    public void saveStateForUndo() {

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
    private float spToPx(float sp) {
        return sp * getResources().getDisplayMetrics().scaledDensity;
    }

}
