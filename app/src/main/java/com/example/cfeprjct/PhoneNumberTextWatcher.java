package com.example.cfeprjct;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

public class PhoneNumberTextWatcher implements TextWatcher {

    private final EditText editText;
    private boolean isFormatting;
    private boolean deletingHyphen;

    public PhoneNumberTextWatcher(EditText editText) {
        this.editText = editText;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        deletingHyphen = count > after;
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (isFormatting || deletingHyphen) {
            return;
        }

        isFormatting = true;

        String raw = s.toString().replaceAll("\\D", "");
        if (raw.length() > 11) {
            raw = raw.substring(0, 11);
        }

        String formatted = "+7 ";
        if (raw.length() > 1) {
            formatted += "(" + raw.substring(1, Math.min(4, raw.length()));
        }
        if (raw.length() > 4) {
            formatted += ") " + raw.substring(4, Math.min(7, raw.length()));
        }
        if (raw.length() > 7) {
            formatted += "-" + raw.substring(7, Math.min(9, raw.length()));
        }
        if (raw.length() > 9) {
            formatted += "-" + raw.substring(9);
        }

        editText.setText(formatted);
        editText.setSelection(formatted.length());

        isFormatting = false;
    }

    @Override
    public void afterTextChanged(Editable s) {
    }
}
