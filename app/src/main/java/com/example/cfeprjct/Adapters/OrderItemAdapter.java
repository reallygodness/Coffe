package com.example.cfeprjct.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.cfeprjct.Entities.CartItem;
import com.example.cfeprjct.R;

public class OrderItemAdapter extends ListAdapter<CartItem, OrderItemAdapter.VH> {

    public OrderItemAdapter() {
        super(new DiffUtil.ItemCallback<CartItem>() {
            @Override public boolean areItemsTheSame(@NonNull CartItem a, @NonNull CartItem b) {
                // сравниваем по типу+id+размеру
                return a.getProductType().equals(b.getProductType())
                        && a.getProductId()==b.getProductId()
                        && a.getSize()==b.getSize();
            }
            @Override public boolean areContentsTheSame(@NonNull CartItem a, @NonNull CartItem b) {
                return a.getQuantity()==b.getQuantity()
                        && a.getUnitPrice()==b.getUnitPrice();
            }
        });
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cart, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        CartItem ci = getItem(pos);
        // тут вся ваша логика заполнения item_cart: изображение, название, размер, qty и цена
        h.title.setText(ci.getTitle());
        h.qty.setText("×" + ci.getQuantity());
        h.price.setText((int)(ci.getUnitPrice()*ci.getQuantity()) + " ₽");
        if (ci.getSize()>0) {
            h.size.setVisibility(View.VISIBLE);
            h.size.setText(ci.getProductType().equals("drink")
                    ? ci.getSize() + " ml"
                    : ci.getSize() + " г");
        } else h.size.setVisibility(View.GONE);
        Glide.with(h.image.getContext())
                .load(ci.getImageUrl())
                .placeholder(R.drawable.ic_placeholder)
                .centerCrop()
                .into(h.image);
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView image;
        TextView title, size, qty, price;
        VH(@NonNull View v) {
            super(v);
            image = v.findViewById(R.id.itemImage);
            title = v.findViewById(R.id.itemTitle);
            size  = v.findViewById(R.id.itemSize);
            qty   = v.findViewById(R.id.tvQuantity);
            price = v.findViewById(R.id.itemPrice);
        }
    }
}
