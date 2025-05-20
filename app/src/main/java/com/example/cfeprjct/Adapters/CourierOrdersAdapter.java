package com.example.cfeprjct.Adapters;

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

import java.util.List;
import java.util.Map;

public class CourierOrdersAdapter extends RecyclerView.Adapter<CourierOrdersAdapter.OrderViewHolder> {


    public interface OnTakeOrderClickListener {
        void onTakeOrder(Order order);
    }

    private List<Order> orders;
    private Map<String, String> userNameMap;
    private Map<String, Address> addressMap;
    private OnTakeOrderClickListener listener;

    public CourierOrdersAdapter(List<Order> orders,
                                Map<String, String> userNameMap,
                                Map<String, Address> addressMap,
                                OnTakeOrderClickListener listener) {
        this.orders = orders;
        this.userNameMap = userNameMap;
        this.addressMap = addressMap;
        this.listener = listener;
    }

    public void setUserNameMap(Map<String, String> map) {
        this.userNameMap = map;
    }
    public void setAddressMap(Map<String, Address> map) {
        this.addressMap = map;
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
        holder.bind(order, userNameMap, addressMap, listener);
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

        public void bind(Order order,
                         Map<String, String> userNameMap,
                         Map<String, Address> addressMap,
                         OnTakeOrderClickListener listener) {
            textOrderNumber.setText("Заказ №" + order.getUserOrderNumber());

            // Имя клиента
            String clientName = userNameMap != null ? userNameMap.get(order.getUserId()) : null;
            textOrderClient.setText(clientName != null && !clientName.isEmpty() ? clientName : "—");

            // Адрес
            Address address = addressMap != null ? addressMap.get(order.getUserId()) : null;
            if (address != null) {
                String addr = address.getCity() + ", " + address.getStreet() + " " + address.getHouse();
                if (address.getApartment() != null && !address.getApartment().isEmpty()) {
                    addr += ", кв. " + address.getApartment();
                }
                textOrderAddress.setText(addr);
            } else {
                textOrderAddress.setText("—");
            }

            textOrderSum.setText("Сумма заказа: " + order.getTotalPrice() + " ₽");

            btnTakeOrder.setOnClickListener(v -> {
                if (listener != null) listener.onTakeOrder(order);
            });
        }
    }
}