package com.example.celebrare;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class CanvasDataDTO implements Serializable {
    public long id;
    public String name;
    public List<TextItemDTO> textItems;

    public CanvasDataDTO() {}

    public CanvasDataDTO(CanvasData data) {
        id = data.getId();
        name = data.getName();
        textItems = new ArrayList<>();
        for (CustomCanvasView.TextItem item : data.getTextItems()) {
            textItems.add(new TextItemDTO(item));
        }
    }

    // Convert DTO back to CanvasData
    public CanvasData toCanvasData() {
        CanvasData data = new CanvasData(id, name);
        List<CustomCanvasView.TextItem> items = new ArrayList<>();
        if (textItems != null) {
            for (TextItemDTO t : textItems) {
                items.add(t.toTextItem());
            }
        }
        data.setTextItems(items);
        return data;
    }
}
