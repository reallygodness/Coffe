package com.example.cfeprjct.Activities.Fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.cfeprjct.R;

import javax.annotation.Nullable;

public class AdminProfileFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Можешь использовать уже существующий layout профиля
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }
}
