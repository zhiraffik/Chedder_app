package com.example.chedder_1.ui.notifications;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.chedder_1.R;
import com.example.chedder_1.domain.db.AppDataBase;
import com.example.chedder_1.domain.entity.User;
import com.example.chedder_1.ui.LoginActivity;
import com.example.chedder_1.ui.ManageEmployeesActivity;

public class NotificationsFragment extends Fragment {

    private EditText etLastName, etFirstName, etEmail, etPassword, etPhone, etPosition;
    private Button btnEdit, btnLogout, btnManageEmployees;

    private boolean isEditMode = false;

    private AppDataBase db;
    private SharedPreferences prefs;

    private int currentUserId = -1;
    private User currentUser;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_notifications, container, false);

        prefs = requireContext().getSharedPreferences("auth", Context.MODE_PRIVATE);
        db = AppDataBase.getInstance(requireContext());

        currentUserId = prefs.getInt("userId", -1);
        if (currentUserId == -1) {
            Toast.makeText(requireContext(), "Нет авторизации", Toast.LENGTH_SHORT).show();
        }

        etLastName  = view.findViewById(R.id.etLastName);
        etFirstName = view.findViewById(R.id.etFirstName);
        etEmail     = view.findViewById(R.id.etEmail);
        etPassword  = view.findViewById(R.id.etPassword);
        etPhone     = view.findViewById(R.id.etPhone);
        etPosition  = view.findViewById(R.id.etPosition);

        btnEdit = view.findViewById(R.id.btnEditProfile);
        btnLogout = view.findViewById(R.id.btnLogout);
        btnManageEmployees = view.findViewById(R.id.btnManageEmployees);

        // показать/скрыть кнопку управления сотрудниками
        if (isAdmin()) {
            btnManageEmployees.setVisibility(View.VISIBLE);
            btnManageEmployees.setOnClickListener(v ->
                    startActivity(new Intent(requireContext(), ManageEmployeesActivity.class))
            );
        } else {
            btnManageEmployees.setVisibility(View.GONE);
        }

        loadProfile();

        // стартуем в просмотре
        setEditMode(false);

        btnEdit.setOnClickListener(v -> {
            if (isEditMode) {
                if (validateFields()) {
                    saveProfile();
                    isEditMode = false;
                    setEditMode(false);
                    showInfoDialog("Профиль сохранён");
                }
            } else {
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

    private boolean isAdmin() {
        return "admin".equals(prefs.getString("role", null));
    }

    private void loadProfile() {
        if (currentUserId == -1) return;

        currentUser = db.userDao().getById(currentUserId);
        if (currentUser == null) return;

        etLastName.setText(currentUser.lastName == null ? "" : currentUser.lastName);
        etFirstName.setText(currentUser.firstName == null ? "" : currentUser.firstName);
        etEmail.setText(currentUser.email == null ? "" : currentUser.email);
        etPassword.setText(currentUser.password == null ? "" : currentUser.password);
        etPhone.setText(currentUser.phone == null ? "" : currentUser.phone);

        // должность: админ — "Администратор", сотрудник — его position
        String positionText = isAdmin() ? "Администратор" : (currentUser.position == null ? "" : currentUser.position);
        etPosition.setText(positionText);
    }

    private void saveProfile() {
        if (currentUser == null) return;

        String lastName = etLastName.getText().toString().trim();
        String firstName = etFirstName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();
        String phone = etPhone.getText().toString().trim();

        currentUser.lastName = lastName;
        currentUser.firstName = firstName;
        currentUser.email = email;
        currentUser.password = password;
        currentUser.phone = phone;

        db.userDao().update(currentUser);
    }

    private void setEditMode(boolean enabled) {
        // позицию не редактируем в профиле (её меняет админ через управление сотрудниками)
        setFieldEditable(etLastName, enabled);
        setFieldEditable(etFirstName, enabled);
        setFieldEditable(etEmail, enabled);
        setFieldEditable(etPassword, enabled);
        setFieldEditable(etPhone, enabled);

        etPosition.setEnabled(false);
        etPosition.setFocusable(false);
        etPosition.setFocusableInTouchMode(false);

        btnEdit.setText(enabled ? "Сохранить" : "Редактировать");
    }

    private void setFieldEditable(EditText editText, boolean enabled) {
        editText.setEnabled(enabled);
        editText.setFocusable(enabled);
        editText.setFocusableInTouchMode(enabled);
    }

    private boolean validateFields() {
        boolean ok = true;

        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString();

        if (email.isEmpty()) {
            etEmail.setError("Введите e-mail");
            ok = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Некорректный e-mail");
            ok = false;
        } else {
            etEmail.setError(null);
        }

        if (password == null || password.length() < 4) {
            etPassword.setError("Минимум 4 символа");
            ok = false;
        } else {
            etPassword.setError(null);
        }

        if (phone.isEmpty()) {
            etPhone.setError("Введите телефон");
            ok = false;
        } else {
            etPhone.setError(null);
        }

        if (!ok) showInfoDialog("Проверьте правильность полей");

        return ok;
    }

    private void showInfoDialog(String msg) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Успешно")
                .setMessage(msg)
                .setPositiveButton("OK", null)
                .show();
    }
}
