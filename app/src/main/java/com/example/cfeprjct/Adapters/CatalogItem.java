// File: com/example/cfeprjct/Adapters/CatalogItem.java
package com.example.cfeprjct.Adapters;

import com.example.cfeprjct.R;

import java.io.Serializable;

public class CatalogItem implements Serializable {
    private final int id;
    private final String title;
    private final String description;
    private final int price;
    private final String category;
    private final String imageUrl;

    private String defaultVolume;  // например "S", "M" или "L"

    private int volumeId;

    private Float rating;

    private final int    size;    // ← добавили

    private int defaultSizeButtonId;




    public CatalogItem(int id,
                       String title,
                       String description,
                       int price,
                       String category,
                       String imageUrl, int size) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.price = price;
        this.category = category;
        this.imageUrl = imageUrl;
        this.size        = size;
        this.rating      = 0f;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public int getPrice() {
        return price;
    }

    public String getCategory() {
        return category;
    }

    public String getImageUrl() {
        return imageUrl;
    }
    public Float getRating() { return rating; }

    public int getSize() { return size; }
    public void setRating(Float rating) { this.rating = rating; }

    public int getDefaultSizeButtonId() {
        if (!"drink".equals(category)) return -1;
        switch (defaultVolume) {
            case "M": return R.id.btnSizeM;
            case "L": return R.id.btnSizeL;
            default:  return R.id.btnSizeS;
        }
    }
    // и, при необходимости, сеттер
    public void setDefaultSizeButtonId(int defaultSizeButtonId) {
        this.defaultSizeButtonId = defaultSizeButtonId;
    }

    public String getType() { return category; }
}
