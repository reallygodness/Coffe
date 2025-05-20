package com.example.cfeprjct.Adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.cfeprjct.Activities.ProductDetailActivity;
import com.example.cfeprjct.R;

import java.util.ArrayList;
import java.util.List;

public class CatalogAdapter extends RecyclerView.Adapter<CatalogAdapter.ViewHolder> {

    private List<CatalogItem> items = new ArrayList<>();

    public void setItems(List<CatalogItem> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_catalog_cart, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CatalogItem item = items.get(position);

        // Заголовок и описание
        holder.titleTextView.setText(item.getTitle());
        holder.descriptionTextView.setText(item.getDescription());

        // Цена
        holder.priceTextView.setText(item.getPrice() + " ₽");

        // Размер (для блюд и десертов показываем в граммах)
        if (("dish".equals(item.getType()) || "dessert".equals(item.getType()))
                && item.getSize() > 0) {
            holder.sizeTextView.setVisibility(View.VISIBLE);
            holder.sizeTextView.setText(item.getSize() + " г");
        } else {
            holder.sizeTextView.setVisibility(View.GONE);
        }

        // Картинка через Glide
        String url = item.getImageUrl();
        if (url != null && !url.isEmpty()) {
            Glide.with(holder.imageView.getContext())
                    .load(url)
                    .placeholder(R.drawable.ic_placeholder)
                    .centerCrop()
                    .into(holder.imageView);
        } else {
            holder.imageView.setImageResource(R.drawable.ic_placeholder);
        }

        // Рейтинг в виде звёздочек
        holder.ratingContainer.removeAllViews();
        Float ratingObj = item.getRating();
        int fullStars = ratingObj != null ? ratingObj.intValue() : 0;
        for (int i = 0; i < fullStars; i++) {
            ImageView star = new ImageView(holder.ratingContainer.getContext());
            star.setImageResource(R.drawable.ic_star);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(2, 0, 2, 0);
            star.setLayoutParams(lp);
            holder.ratingContainer.addView(star);
        }

        // Клик по карточке
        holder.itemView.setOnClickListener(v -> {
            Context ctx = v.getContext();
            Intent intent = new Intent(ctx, ProductDetailActivity.class);
            intent.putExtra(ProductDetailActivity.EXTRA_ITEM, item);
            ctx.startActivity(intent);
        });

    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView    titleTextView,
                descriptionTextView,
                priceTextView,
                sizeTextView;
        ImageView   imageView;
        LinearLayout ratingContainer;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView       = itemView.findViewById(R.id.itemTitleTextView);
            descriptionTextView = itemView.findViewById(R.id.itemDescriptionTextView);
            priceTextView       = itemView.findViewById(R.id.itemPriceTextView);
            sizeTextView        = itemView.findViewById(R.id.itemSize);
            imageView           = itemView.findViewById(R.id.itemImageView);
            ratingContainer     = itemView.findViewById(R.id.ratingContainer);
        }
    }
}
