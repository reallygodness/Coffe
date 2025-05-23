package com.example.cfeprjct.Adapters;

import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cfeprjct.Entities.Address;
import com.example.cfeprjct.Entities.Order;
import com.example.cfeprjct.R;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CourierOrdersAdapter extends RecyclerView.Adapter<CourierOrdersAdapter.OrderViewHolder> {


    public interface OnTakeOrderClickListener {
        void onTakeOrder(Order order);
        void onDelivered(Order order);
    }

    private List<Order> orders;
    private Map<String, String> userNameMap;
    private Map<String, Address> addressMap;
    private String courierId;
    private OnTakeOrderClickListener listener;

    // Для таймеров (ключ — firestoreOrderId, чтобы не было путаницы)
    private final Map<String, CountDownTimer> timerMap = new HashMap<>();

    public CourierOrdersAdapter(List<Order> orders, Map<String, String> userNameMap, Map<String, Address> addressMap, OnTakeOrderClickListener listener) {
        this.orders = orders;
        this.userNameMap = userNameMap;
        this.addressMap = addressMap;
        this.listener = listener;
    }

    public void setUserNameMap(Map<String, String> userNameMap) {
        this.userNameMap = userNameMap;
    }

    public void setAddressMap(Map<String, Address> addressMap) {
        this.addressMap = addressMap;
    }

    public void setCourierId(String courierId) {
        this.courierId = courierId;
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_courier_order, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Order order = orders.get(position);
        holder.bind(order, userNameMap, addressMap, courierId, listener, timerMap);
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    public static class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView textOrderNumber, textOrderClient, textOrderAddress, textOrderSum, textOrderTimer, textOrderStatus;
        Button btnTakeOrder, btnDelivered;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            textOrderNumber = itemView.findViewById(R.id.textOrderNumber);
            textOrderClient = itemView.findViewById(R.id.textOrderClient);
            textOrderAddress = itemView.findViewById(R.id.textOrderAddress);
            textOrderSum = itemView.findViewById(R.id.textOrderSum);
            textOrderTimer = itemView.findViewById(R.id.textOrderTimer);
            textOrderStatus = itemView.findViewById(R.id.textOrderStatus);
            btnTakeOrder = itemView.findViewById(R.id.btnTakeOrder);
            btnDelivered = itemView.findViewById(R.id.btnDelivered);
        }

        public void bind(Order order, Map<String, String> userNameMap, Map<String, Address> addressMap, String courierId,
                         OnTakeOrderClickListener listener, Map<String, CountDownTimer> timerMap) {
            btnTakeOrder.setVisibility(View.GONE);
            btnDelivered.setVisibility(View.GONE);
            textOrderTimer.setVisibility(View.GONE);
            textOrderStatus.setVisibility(View.VISIBLE);

            // Используем firestoreOrderId как номер заказа
            String orderNumber = order.getFirestoreOrderId() != null ? order.getFirestoreOrderId() : "—";
            textOrderNumber.setText("Заказ №" + orderNumber);

            String clientName = userNameMap.getOrDefault(order.getUserId(), "—");
            textOrderClient.setText("Клиент: " + clientName);

            Address addr = addressMap.get(order.getUserId());
            String addrStr = (addr != null) ?
                    addr.getCity() + ", " + addr.getStreet() + " " + addr.getHouse() +
                            (addr.getApartment() != null && !addr.getApartment().isEmpty() ? ", кв. " + addr.getApartment() : "")
                    : "—";
            textOrderAddress.setText("Адрес: " + addrStr);

            textOrderSum.setText("Сумма заказа: " + order.getTotalPrice() + " ₽");

            long now = System.currentTimeMillis();
            // Очищаем старый таймер по id заказа
            if (order.getFirestoreOrderId() != null && timerMap.containsKey(order.getFirestoreOrderId())) {
                timerMap.get(order.getFirestoreOrderId()).cancel();
                timerMap.remove(order.getFirestoreOrderId());
            }

            // ---------- ЛОГИКА СТАТУСОВ ----------
            if (order.getStatusId() == 1) { // В готовке
                if (order.getCourierId() == null || order.getCourierId().isEmpty()) {
                    // Свободен
                    btnTakeOrder.setVisibility(View.VISIBLE);
                    btnTakeOrder.setOnClickListener(v -> listener.onTakeOrder(order));
                    textOrderStatus.setText("Статус: В готовке (свободен)");
                } else if (order.getCourierId().equals(courierId)) {
                    // Взят этим курьером, показываем таймер до конца готовки
                    textOrderTimer.setVisibility(View.VISIBLE);
                    long takeTime = order.getCourierTakeTime() != null ? order.getCourierTakeTime() : now;
                    long msLeft = (takeTime + 5 * 60 * 1000) - now;
                    if (msLeft > 0) {
                        textOrderStatus.setText("Статус: Ожидание приготовления");
                        startCountDownTimer(msLeft, textOrderTimer, () -> {
                            // По истечении времени меняем статус на "В доставке"
                            updateOrderStatusToDelivery(order.getFirestoreOrderId());
                        }, timerMap, order.getFirestoreOrderId());
                    } else {
                        textOrderStatus.setText("Статус: Ожидание статуса доставки...");
                        updateOrderStatusToDelivery(order.getFirestoreOrderId());
                    }
                } else {
                    textOrderStatus.setText("Статус: В готовке (занят)");
                }
            } else if (order.getStatusId() == 2 && order.getCourierId().equals(courierId)) {
                // В доставке этим курьером (20 минут)
                textOrderTimer.setVisibility(View.VISIBLE);
                btnDelivered.setVisibility(View.VISIBLE);
                btnDelivered.setOnClickListener(v -> listener.onDelivered(order));
                textOrderStatus.setText("Статус: В доставке");
                long deliveryStartTime = order.getDeliveryStartTime() != null ? order.getDeliveryStartTime() : now;
                long msLeft = (deliveryStartTime + 20 * 60 * 1000) - now;
                if (msLeft > 0) {
                    startCountDownTimer(msLeft, textOrderTimer, null, timerMap, order.getFirestoreOrderId());
                } else {
                    textOrderTimer.setText("00:00");
                }
            } else if (order.getStatusId() == 3 && order.getCourierId().equals(courierId)) {
                textOrderStatus.setText("Статус: Доставлен и оплачен");
                textOrderTimer.setVisibility(View.GONE);
                btnDelivered.setVisibility(View.GONE);
            } else {
                textOrderStatus.setText("Статус: —");
            }
        }

        private void startCountDownTimer(long millisInFuture, TextView timerView, Runnable onFinish,
                                         Map<String, CountDownTimer> timerMap, String orderId) {
            CountDownTimer timer = new CountDownTimer(millisInFuture, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    long mins = millisUntilFinished / 60000;
                    long secs = (millisUntilFinished / 1000) % 60;
                    timerView.setText(String.format("%02d:%02d", mins, secs));
                }

                @Override
                public void onFinish() {
                    timerView.setText("00:00");
                    if (onFinish != null) onFinish.run();
                }
            }.start();
            if (orderId != null)
                timerMap.put(orderId, timer);
        }

        private void updateOrderStatusToDelivery(String firestoreOrderId) {
            FirebaseFirestore.getInstance().collection("orders")
                    .document(firestoreOrderId)
                    .update("statusId", 2, "deliveryStartTime", System.currentTimeMillis());
        }
    }
}