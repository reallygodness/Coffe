package com.example.cfeprjct.Activities.Fragments;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.cfeprjct.R;

public class EditAddressDialogFragment extends DialogFragment {
    @NonNull @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View v = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_edit_address, null);
        EditText etCity      = v.findViewById(R.id.etCity);
        EditText etStreet    = v.findViewById(R.id.etStreet);
        EditText etHouse     = v.findViewById(R.id.etHouse);
        EditText etApartment = v.findViewById(R.id.etApartment);

        return new AlertDialog.Builder(requireContext())
                .setTitle("Редактировать адрес")
                .setView(v)
                .setPositiveButton("Сохранить", (d, w) -> {
                    // TODO: вызвать метод у фрагмента CartFragment,
                    // чтобы обновить Room + Firestore и UI
                })
                .setNegativeButton("Отмена", null)
                .create();
    }
}
