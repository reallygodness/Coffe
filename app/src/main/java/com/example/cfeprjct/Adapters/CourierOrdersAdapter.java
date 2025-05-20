package com.example.cfeprjct.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cfeprjct.Entities.Order;
import com.example.cfeprjct.R;

import java.util.List;

public class CourierOrdersAdapter extends RecyclerView.Adapter<CourierOrdersAdapter.OrderViewHolder> {

    public interface OnTakeOrderClickListener {
        void onTakeOrder(Order order);
    }

    private List<Order> orders;
    private OnTakeOrderClickListener listener;

    public CourierOrdersAdapter(List<Order> orders, OnTakeOrderClickListener listener) {
        this.orders = orders;
        this.listener = listener;
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
        holder.bind(order, listener);
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    public static class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView textOrderNumber, textOrderClient, textOrderAddress, textOrderSum;
        Button btnTakeOrder;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            textOrderNumber = itemView.findViewById(R.id.textOrderNumber);
            textOrderClient = itemView.findViewById(R.id.textOrderClient);
            textOrderAddress = itemView.findViewById(R.id.textOrderAddress);
            textOrderSum = itemView.findViewById(R.id.textOrderSum);
            btnTakeOrder = itemView.findViewById(R.id.btnTakeOrder);
        }

        public void bind(Order order, OnTakeOrderClickListener listener) {
            textOrderNumber.setText("Заказ №" + order.getOrderNumber());
            textOrderClient.setText(order.getClientName() != null ? order.getClientName() : "—");
            textOrderAddress.setText(order.getAddress() != null ? order.getAddress() : "—");
            textOrderSum.setText("Сумма заказа: " + order.getTotalPrice() + " ₽");

            btnTakeOrder.setOnClickListener(v -> {
                if (listener != null) listener.onTakeOrder(order);
            });
        }
    }
}