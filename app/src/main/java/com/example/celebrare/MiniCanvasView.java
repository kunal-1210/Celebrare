// MiniCanvasView.java
package com.example.celebrare;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.compose.ui.graphics.Shape;

import com.example.celebrare.CanvasData;

public class MiniCanvasView extends View {
    private CanvasData sheetData;
    private Paint paint = new Paint();

    public MiniCanvasView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setSheetData(CanvasData data) {
        this.sheetData = data;
        if (data != null) {
            Log.d("MiniCanvasView", "setSheetData called: " + data.getName()
                + ", text items: " + (data.getTextItems() != null ? data.getTextItems().size() : 0));
        } else {
            Log.d("MiniCanvasView", "setSheetData called with null data");
        }
        invalidate(); // redraw
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (sheetData == null) {
            Log.d("MiniCanvasView", "onDraw: sheetData is null");
            return;
        }
        if (sheetData.getTextItems() == null || sheetData.getTextItems().isEmpty()) {
            Log.d("MiniCanvasView", "onDraw: textItems is null or empty");
            return;
        }

        Log.d("MiniCanvasView", "onDraw: drawing " + sheetData.getTextItems().size() + " items");
        float originalWidth = 800f;   // adjust if your full-size canvas is different
        float originalHeight = 1200f;

        float scaleX = getWidth() / originalWidth;
        float scaleY = getHeight() / originalHeight;

        float scale = Math.min(scaleX, scaleY);

        for (CustomCanvasView.TextItem item : sheetData.getTextItems()) {
            Log.d("MiniCanvasView", "Drawing text: " + item.text + " at (" + item.x + "," + item.y + ")");
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setColor(item.color);
            paint.setTextSize(item.size * scale);
            paint.setTypeface(item.typeface != null ? item.typeface : Typeface.DEFAULT);
            paint.setUnderlineText(item.underline);

            // scale the position
            float x = item.x * scale;
            float y = item.y * scale;

            canvas.drawText(item.text, x, y, paint);
        }
    }
}
