package com.example.celebrare;

import java.io.Serializable;
import android.graphics.Paint;

public class TextItemDTO implements Serializable {
    public String text;
    public float x, y, size;
    public boolean bold, italic, underline;
    public int color;
    public String fontName;
    public long id;

    public TextItemDTO() {}

    public TextItemDTO(CustomCanvasView.TextItem item) {
        this.text = item.text;
        this.x = item.x;
        this.y = item.y;
        this.size = item.size;
        this.bold = item.bold;
        this.italic = item.italic;
        this.underline = item.underline;
        this.color = item.color;
        this.fontName = item.fontName;
        this.id = item.id;
    }

    // Convert DTO back to a real TextItem
    public CustomCanvasView.TextItem toTextItem() {
        return new CustomCanvasView.TextItem(
            text, x, y, fontName, size, bold, italic, underline, color,
            Paint.Align.CENTER, null, id
        );
    }
}
