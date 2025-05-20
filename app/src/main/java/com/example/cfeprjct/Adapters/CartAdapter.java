package com.example.cfeprjct.Adapters;

import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.cfeprjct.DAOS.CartItemDAO;
import com.example.cfeprjct.Entities.CartItem;
import com.example.cfeprjct.R;

import java.util.concurrent.Executors;

public class CartAdapter extends ListAdapter<CartItem, CartAdapter.VH> {

    private final CartItemDAO dao;

    public CartAdapter(@NonNull CartItemDAO dao) {
        super(new DiffUtil.ItemCallback<CartItem>() {
            @Override
            public boolean areItemsTheSame(@NonNull CartItem a, @NonNull CartItem b) {
                return a.getProductType().equals(b.getProductType())
                        && a.getProductId() == b.getProductId()
                        && a.getSize() == b.getSize();
            }

            @Override
            public boolean areContentsTheSame(@NonNull CartItem a, @NonNull CartItem b) {
                return a.getQuantity() == b.getQuantity()
                        && Float.compare(a.getUnitPrice(), b.getUnitPrice()) == 0
                        && a.getSize() == b.getSize()
                        && a.getTitle().equals(b.getTitle()); // добавь это для надёжности
            }
        });
        this.dao = dao;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cart, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int i) {
        CartItem ci = getItem(i);

        h.title.setText(ci.getTitle());
        h.qty.setText(String.valueOf(ci.getQuantity()));
        h.price.setText((int)(ci.getUnitPrice() * ci.getQuantity()) + " ₽");

        if (ci.getSize() > 0) {
            h.size.setText(ci.getProductType().equals("drink")
                    ? (ci.getSize() + " ml")
                    : (ci.getSize() + " г"));
            h.size.setVisibility(View.VISIBLE);
        } else {
            h.size.setVisibility(View.GONE);
        }

        Glide.with(h.image.getContext())
                .load(ci.getImageUrl())
                .placeholder(R.drawable.ic_placeholder)
                .centerCrop()
                .into(h.image);

        // — Уменьшить количество —
        h.btnDecrease.setOnClickListener(v -> {
            int newQ = ci.getQuantity() - 1;
            if (newQ < 1) return;
            ci.setQuantity(newQ);
            new Thread(() -> {
                dao.update(ci);
                new Handler(Looper.getMainLooper()).post(this::notifyDataSetChanged);
            }).start();
        });

        // — Увеличить количество (до 15) —
        h.btnIncrease.setOnClickListener(v -> {
            int newQ = Math.min(ci.getQuantity() + 1, 15);
            ci.setQuantity(newQ);
            new Thread(() -> {
                dao.update(ci);
                new Handler(Looper.getMainLooper()).post(this::notifyDataSetChanged);
            }).start();
        });

        // — Удалить элемент из корзины —
        h.btnDelete.setOnClickListener(v ->
                new Thread(() -> {
                    dao.delete(ci);
                    new Handler(Looper.getMainLooper()).post(this::notifyDataSetChanged);
                }).start()
        );
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView image;
        TextView title, size, qty, price;
        ImageButton btnDecrease, btnIncrease, btnDelete;

        VH(@NonNull View v) {
            super(v);
            image       = v.findViewById(R.id.itemImage);
            title       = v.findViewById(R.id.itemTitle);
            size        = v.findViewById(R.id.itemSize);
            qty         = v.findViewById(R.id.tvQuantity);
            price       = v.findViewById(R.id.itemPrice);
            btnDecrease = v.findViewById(R.id.btnDecrease);
            btnIncrease = v.findViewById(R.id.btnIncrease);
            btnDelete   = v.findViewById(R.id.btnRemove);
        }

        // меняем в DiffCallback:
        public static final DiffUtil.ItemCallback<CartItem> DIFF_CALLBACK =
                new DiffUtil.ItemCallback<CartItem>() {
                    @Override public boolean areItemsTheSame(@NonNull CartItem a, @NonNull CartItem b) {
                        return a.getProductType().equals(b.getProductType())
                                && a.getProductId() == b.getProductId()
                                && a.getSize()      == b.getSize();
                    }
                    @Override public boolean areContentsTheSame(@NonNull CartItem a, @NonNull CartItem b) {
                        return a.getQuantity() == b.getQuantity()
                                && a.getUnitPrice() == b.getUnitPrice()
                                && a.getSize()      == b.getSize();
                    }
                };
    }
}
