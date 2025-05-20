package com.example.cfeprjct;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

public class Utils {

    // Маска для ввода номера телефона в EditText (для профиля и авторизации)
    public static void setPhoneMask(final EditText editText) {
        editText.addTextChangedListener(new TextWatcher() {
            private boolean isEditing = false;
            private final String mask = "+7 (XXX) XXX-XX-XX";

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isEditing || s.length() < 3) return;
                isEditing = true;

                String clean = s.toString().replaceAll("[^\\d]", "");
                if (clean.startsWith("7")) clean = clean.substring(1);
                if (clean.length() > 10) clean = clean.substring(0, 10);

                StringBuilder formatted = new StringBuilder("+7 ");
                for (int i = 0, j = 0; i < clean.length() && j < mask.length(); j++) {
                    if (mask.charAt(j) == 'X') {
                        formatted.append(clean.charAt(i));
                        i++;
                    } else {
                        formatted.append(mask.charAt(j));
                    }
                }

                editText.setText(formatted.toString());
                editText.setSelection(formatted.length());
                isEditing = false;
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    public static String formatPhoneNumber(String phone) {
        if (phone == null || phone.length() != 11) return phone;
        return "+7 (" + phone.substring(1, 4) + ") " + phone.substring(4, 7) + "-" + phone.substring(7, 9) + "-" + phone.substring(9, 11);
    }
}
