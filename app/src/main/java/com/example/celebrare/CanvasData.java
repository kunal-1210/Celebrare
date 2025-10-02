package com.example.celebrare;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CanvasData  implements Serializable{
    private final long id;
    private String name;
    private List<CustomCanvasView.TextItem> textItems = new ArrayList<>();

    public Map<String, Object> toMap() {
        Map<String, Object> sheetMap = new HashMap<>();
        sheetMap.put("name", name);

        Map<String, Object> itemsMap = new HashMap<>();
        for (CustomCanvasView.TextItem item : textItems) {
            Map<String, Object> itemMap = new HashMap<>();
            itemMap.put("content", item.text);
            itemMap.put("x", item.x);
            itemMap.put("y", item.y);
            itemMap.put("size", item.size);
            itemMap.put("bold", item.bold);
            itemMap.put("italic", item.italic);
            itemMap.put("underline", item.underline);
            itemMap.put("color", item.color);
            itemMap.put("fontName", item.fontName);
            itemsMap.put(String.valueOf(item.id), itemMap); // key = textItemId
        }
        sheetMap.put("textItems", itemsMap);

        return sheetMap;
    }


    public CanvasData(long id, String name) {
        this.id = id;
        this.name = name;
    }

    public long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<CustomCanvasView.TextItem> getTextItems() { return textItems; }

    public void setTextItems(List<CustomCanvasView.TextItem> textItems) {
        this.textItems = textItems;
    }
}
