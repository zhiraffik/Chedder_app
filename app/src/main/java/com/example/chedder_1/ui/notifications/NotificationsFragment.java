package com.example.chedder_1.ui.notifications;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.chedder_1.ui.LoginActivity;
import com.example.chedder_1.R;

public class NotificationsFragment extends Fragment {

    private EditText etLastName, etFirstName, etMiddleName, etEmail, etPhone;
    private Button btnEdit, btnLogout;

    private boolean isEditMode = false; // false = просмотр, true = редактирование

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_notifications, container, false);

        etLastName   = view.findViewById(R.id.etLastName);
        etFirstName  = view.findViewById(R.id.etFirstName);
        etMiddleName = view.findViewById(R.id.etMiddleName);
        etEmail      = view.findViewById(R.id.etEmail);
        etPhone      = view.findViewById(R.id.etPhone);

        btnEdit   = view.findViewById(R.id.btnEditProfile);
        btnLogout = view.findViewById(R.id.btnLogout);

        // стартуем в режиме просмотра
        setEditMode(false);

        btnEdit.setOnClickListener(v -> {
            if (isEditMode) {
                // сейчас режим редактирования → пытаемся сохранить
                if (validateFields()) {
                    isEditMode = false;
                    setEditMode(false);
                    showInfoDialog("Профиль сохранён");
                    // здесь можно реально сохранить в БД / SharedPreferences
                }
            } else {
                // включаем редактирование
                isEditMode = true;
                setEditMode(true);
            }
        });
        btnLogout.setOnClickListener(v -> {
            requireActivity()
                    .getSharedPreferences("auth", Context.MODE_PRIVATE)
                    .edit()
                    .clear()
                    .apply();

            startActivity(new Intent(requireContext(), LoginActivity.class));
            requireActivity().finish();
        });
        return view;
    }




    /** Вкл/выкл редактирование полей */
    private void setEditMode(boolean enabled) {
        setFieldEditable(etLastName,   enabled);
        setFieldEditable(etFirstName,  enabled);
        setFieldEditable(etMiddleName, enabled);
        setFieldEditable(etEmail,      enabled);
        setFieldEditable(etPhone,      enabled);

        btnEdit.setText(enabled ? "Сохранить" : "Редактировать");
    }

    private void setFieldEditable(EditText editText, boolean enabled) {
        editText.setEnabled(enabled);
        editText.setFocusable(enabled);
        editText.setFocusableInTouchMode(enabled);
    }

    /** Проверка e-mail и телефона */
    private boolean validateFields() {
        boolean ok = true;

        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        // E-mail
        if (email.isEmpty()) {
            etEmail.setError("Введите e-mail");
            ok = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Некорректный e-mail");
            ok = false;
        } else {
            etEmail.setError(null);
        }

        // Телефон: только цифры, длина = 10
        String digitsOnly = phone.replaceAll("\\D", ""); // только цифры

        if (digitsOnly.length() != 10) {
            etPhone.setError("Введите 10 цифр (без +7)");
            ok = false;
        } else {
            etPhone.setError(null);
        }

        if (!ok) {
            showInfoDialog("Проверьте правильность полей");
        }

        return ok;
    }

    /** Автоматически добавляет +7 перед номером */
    private void formatPhoneNumber() {
        String phone = etPhone.getText().toString().trim();
        String digits = phone.replaceAll("\\D", "");

        if (digits.length() == 10) {
            etPhone.setText("+7" + digits);
        }
    }

    private void showInfoDialog(String msg) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Успешно")
                .setMessage(msg)
                .setPositiveButton("OK", null)
                .show();
    }
}
