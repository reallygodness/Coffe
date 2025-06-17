package com.example.cfeprjct.Adapters;

import android.content.Context;
import android.os.Handler;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.cfeprjct.AppDatabase;
import com.example.cfeprjct.Entities.Address;
import com.example.cfeprjct.Entities.CartItem;
import com.example.cfeprjct.Entities.Order;
import com.example.cfeprjct.Entities.OrderStatus;
import com.example.cfeprjct.Entities.OrderedDessert;
import com.example.cfeprjct.Entities.OrderedDish;
import com.example.cfeprjct.Entities.OrderedDrink;
import com.example.cfeprjct.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;

public class OrderAdapter extends ListAdapter<Order, OrderAdapter.VH> {

    private final LayoutInflater inflater;
    private final AppDatabase db;
    private final List<OrderStatus> statuses;

    public OrderAdapter(Context ctx, List<OrderStatus> statuses) {
        super(new DiffUtil.ItemCallback<Order>() {
            @Override public boolean areItemsTheSame(@NonNull Order a, @NonNull Order b) {
                return a.getOrderId() == b.getOrderId();
            }
            @Override public boolean areContentsTheSame(@NonNull Order a, @NonNull Order b) {
                return a.getCreatedAt() == b.getCreatedAt()
                        && a.getTotalPrice() == b.getTotalPrice()
                        && a.getStatusId() == b.getStatusId();
            }
        });
        this.inflater  = LayoutInflater.from(ctx);
        this.db        = AppDatabase.getInstance(ctx);
        this.statuses  = statuses;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = inflater.inflate(R.layout.item_order, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull final VH holder, int position) {
        final Order order = getItem(position);

        // 1) Заполняем базовые поля
        holder.tvOrderId.setText("Заказ №" + order.getOrderId());
        holder.tvDate.setText(DateFormat.format("dd.MM.yy HH:mm", new Date(order.getCreatedAt())));
        holder.tvPayment.setText("К оплате курьеру: " + (int) order.getTotalPrice() + " ₽");
        // статус и таймер выставятся далее

        // 2) Адрес доставки
        Executors.newSingleThreadExecutor().execute(() -> {
            Address addr = db.addressDAO().getAddressByUserId(order.getUserId());
            String formatted = addr != null
                    ? addr.getCity() + ", " + addr.getStreet() + " " + addr.getHouse()
                    + (addr.getApartment().isEmpty() ? "" : ", кв. " + addr.getApartment())
                    : "Адрес не задан";
            holder.itemView.post(() -> holder.tvAddress.setText(formatted));
        });

        // 3) Список товаров в заказе
        holder.itemsContainer.removeAllViews();
        Executors.newSingleThreadExecutor().execute(() -> {
            List<CartItem> items = new ArrayList<>();

            boolean hasPositions = false; // был ли найден хотя бы 1 товар

            for (OrderedDrink od : db.orderedDrinkDAO().getByOrderId(order.getOrderId())) {
                var d = db.drinkDAO().getById(od.getDrinkId());
                if (d != null) {
                    CartItem ci = new CartItem();
                    ci.setProductType("drink");
                    ci.setTitle(d.getName());
                    ci.setImageUrl(d.getImageUrl());
                    ci.setQuantity(od.getQuantity());
                    ci.setUnitPrice(db.priceListDAO().getLatestPriceForDrink(d.getDrinkId()));
                    ci.setSize(od.getSize());
                    items.add(ci);
                    hasPositions = true;
                }
            }
            for (OrderedDish od : db.orderedDishDAO().getByOrderId(order.getOrderId())) {
                var d = db.dishDAO().getById(od.getDishId());
                if (d != null) {
                    CartItem ci = new CartItem();
                    ci.setProductType("dish");
                    ci.setTitle(d.getName());
                    ci.setImageUrl(d.getImageUrl());
                    ci.setQuantity(od.getQuantity());
                    ci.setUnitPrice(db.priceListDAO().getLatestPriceForDish(d.getDishId()));
                    ci.setSize(od.getSize());
                    items.add(ci);
                    hasPositions = true;
                }
            }
            for (OrderedDessert od : db.orderedDessertDAO().getByOrderId(order.getOrderId())) {
                var d = db.dessertDAO().getById(od.getDessertId());
                if (d != null) {
                    CartItem ci = new CartItem();
                    ci.setProductType("dessert");
                    ci.setTitle(d.getName());
                    ci.setImageUrl(d.getImageUrl());
                    ci.setQuantity(od.getQuantity());
                    ci.setUnitPrice(db.priceListDAO().getLatestPriceForDessert(d.getDessertId()));
                    ci.setSize(od.getSize());
                    items.add(ci);
                    hasPositions = true;
                }
            }

            // Если ни одного товара не найдено, пробуем обновить позиции заказа из Firestore
            if (!hasPositions) {
                FirebaseFirestore firestore = FirebaseFirestore.getInstance();
                firestore.collection("orders")
                        .document(String.valueOf(order.getOrderId()))
                        .collection("drinks")
                        .get()
                        .addOnSuccessListener(drinkSnap -> {
                            Executors.newSingleThreadExecutor().execute(() -> {
                                for (DocumentSnapshot d : drinkSnap.getDocuments()) {
                                    OrderedDrink od = new OrderedDrink();
                                    od.setOrderedDrinkId(d.getLong("orderedDrinkId").intValue());
                                    od.setOrderId(d.getLong("orderId").intValue());
                                    od.setDrinkId(d.getLong("drinkId").intValue());
                                    od.setQuantity(d.getLong("quantity").intValue());
                                    od.setSize(d.getLong("size").intValue());
                                    db.orderedDrinkDAO().insert(od);
                                }
                            });
                        });
                firestore.collection("orders")
                        .document(String.valueOf(order.getOrderId()))
                        .collection("dishes")
                        .get()
                        .addOnSuccessListener(dishSnap -> {
                            Executors.newSingleThreadExecutor().execute(() -> {
                                for (DocumentSnapshot d : dishSnap.getDocuments()) {
                                    OrderedDish od = new OrderedDish();
                                    od.setOrderedDishId(d.getLong("orderedDishId").intValue());
                                    od.setOrderId(d.getLong("orderId").intValue());
                                    od.setDishId(d.getLong("dishId").intValue());
                                    od.setQuantity(d.getLong("quantity").intValue());
                                    od.setSize(d.getLong("size").intValue());
                                    db.orderedDishDAO().insert(od);
                                }
                            });
                        });
                firestore.collection("orders")
                        .document(String.valueOf(order.getOrderId()))
                        .collection("desserts")
                        .get()
                        .addOnSuccessListener(desSnap -> {
                            Executors.newSingleThreadExecutor().execute(() -> {
                                for (DocumentSnapshot d : desSnap.getDocuments()) {
                                    OrderedDessert od = new OrderedDessert();
                                    od.setOrderedDessertId(d.getLong("orderedDessertId").intValue());
                                    od.setOrderId(d.getLong("orderId").intValue());
                                    od.setDessertId(d.getLong("dessertId").intValue());
                                    od.setQuantity(d.getLong("quantity").intValue());
                                    od.setSize(d.getLong("size").intValue());
                                    db.orderedDessertDAO().insert(od);
                                }
                            });
                        });
                // После синхронизации можно обновить адаптер (например, через notifyDataSetChanged)
            }


            holder.itemView.post(() -> {
                for (CartItem ci : items) {
                    View iv = inflater.inflate(R.layout.item_cart, holder.itemsContainer, false);

                    // скрываем лишнее из корзины
                    View btnDec = iv.findViewById(R.id.btnDecrease);
                    View btnInc = iv.findViewById(R.id.btnIncrease);
                    View btnRemove = iv.findViewById(R.id.btnRemove);
                    if (btnDec != null) btnDec.setVisibility(View.GONE);
                    if (btnInc != null) btnInc.setVisibility(View.GONE);
                    if (btnRemove != null) btnRemove.setVisibility(View.GONE);

                    ImageView img = iv.findViewById(R.id.itemImage);
                    TextView title = iv.findViewById(R.id.itemTitle);
                    TextView size = iv.findViewById(R.id.itemSize);
                    TextView qty = iv.findViewById(R.id.tvQuantity);
                    TextView price = iv.findViewById(R.id.itemPrice);

                    title.setText(ci.getTitle());
                    qty.setText("×" + ci.getQuantity());
                    price.setText((int) (ci.getUnitPrice() * ci.getQuantity()) + " ₽");
                    if (ci.getSize() > 0) {
                        size.setVisibility(View.VISIBLE);
                        size.setText(ci.getSize()
                                + (ci.getProductType().equals("drink") ? " ml" : " г"));
                    } else {
                        size.setVisibility(View.GONE);
                    }

                    Glide.with(img.getContext())
                            .load(ci.getImageUrl())
                            .placeholder(R.drawable.ic_placeholder)
                            .centerCrop()
                            .into(img);

                    holder.itemsContainer.addView(iv);
                }
            });
        });

        // ======= 4) Логика таймера статусов СИНХРОНИЗИРОВАНА =======
        final Handler handler = new Handler(holder.itemView.getContext().getMainLooper());
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();

                if (order.getStatusId() == 1) {
                    long elapsed = now - order.getCreatedAt();
                    long prepMs = 5 * 60_000;
                    if (elapsed < prepMs) {
                        holder.tvStatus.setText("В готовке");
                        long rem = prepMs - elapsed;
                        int m = (int) (rem / 60_000);
                        int s = (int) ((rem % 60_000) / 1000);
                        holder.tvTimer.setVisibility(View.VISIBLE);
                        holder.tvTimer.setText(String.format("Готовка: %02d:%02d", m, s));
                        handler.postDelayed(this, 1000);
                    } else {
                        // Здесь можно обновить флаг в заказе, чтобы он стал доступен курьерам (например, через Firestore)
                        holder.tvStatus.setText("Ожидает курьера");
                        holder.tvTimer.setVisibility(View.GONE);
                    }
                } else if (order.getStatusId() == 2 && order.getDeliveryStartTime() != null) {
                    long elapsed = now - order.getDeliveryStartTime();
                    long delivMs = 20 * 60_000;
                    if (elapsed < delivMs) {
                        holder.tvStatus.setText("В доставке");
                        long rem = delivMs - elapsed;
                        int m = (int) (rem / 60_000);
                        int s = (int) ((rem % 60_000) / 1000);
                        holder.tvTimer.setVisibility(View.VISIBLE);
                        holder.tvTimer.setText(String.format("Доставка: %02d:%02d", m, s));
                        handler.postDelayed(this, 1000);
                    } else {
                        holder.tvStatus.setText("В доставке (ожидается подтверждение)");
                        holder.tvTimer.setVisibility(View.GONE);
                    }
                } else if (order.getStatusId() == 3) {
                    holder.tvStatus.setText("Доставлен и оплачен");
                    holder.tvTimer.setVisibility(View.GONE);
                }
            }
        };
        handler.post(runnable);
    }

        static class VH extends RecyclerView.ViewHolder {
        TextView     tvOrderId, tvDate, tvStatus, tvPayment, tvAddress, tvTimer;
        LinearLayout itemsContainer;
        VH(View v) {
            super(v);
            tvOrderId      = v.findViewById(R.id.tvOrderId);
            tvDate         = v.findViewById(R.id.tvOrderDate);
            tvStatus       = v.findViewById(R.id.tvOrderStatus);
            tvPayment      = v.findViewById(R.id.tvOrderPayment);
            itemsContainer = v.findViewById(R.id.itemsContainer);
            tvAddress      = v.findViewById(R.id.tvOrderAddress);
            tvTimer        = v.findViewById(R.id.tvOrderTimer);
        }
    }
}
