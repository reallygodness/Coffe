package com.example.cfeprjct.Activities.Fragments;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.cfeprjct.R;

public class OrderConfirmationDialogFragment extends DialogFragment {
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View v = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_order_confirm, null);

        return new AlertDialog.Builder(requireContext())
                .setView(v)
                .setCancelable(false)
                .setPositiveButton("Перейти в заказы", (dialog, which) -> {
                    // Заменяем текущий фрагмент в frame_container на OrdersFragment
                    requireActivity().getSupportFragmentManager()
                            .beginTransaction()
                            .replace(
                                    R.id.frame_container,    // именно этот ID у вас в activity_main.xml
                                    new OrdersFragment()
                            )
                            .addToBackStack(null)
                            .commit();
                })
                .create();
    }
}
