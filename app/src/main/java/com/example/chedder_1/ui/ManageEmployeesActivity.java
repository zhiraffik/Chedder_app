package com.example.chedder_1.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.util.Patterns;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.chedder_1.R;
import com.example.chedder_1.domain.db.AppDataBase;
import com.example.chedder_1.domain.entity.User;

import java.util.ArrayList;
import java.util.List;

public class ManageEmployeesActivity extends AppCompatActivity {

    private Spinner spEmployee;

    private EditText etFirstName, etLastName, etEmail, etPassword, etPhone, etRate, etHours;
    private Spinner spPosition;

    private Button btnTogglePassword;
    private boolean isPasswordVisible = false;

    private Button btnSave, btnDelete;

    private AppDataBase db;
    private SharedPreferences prefs;

    private int adminId = -1;

    private List<User> staff = new ArrayList<>();
    private User currentUser = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_employees);

        db = AppDataBase.getInstance(this);
        prefs = getSharedPreferences("auth", Context.MODE_PRIVATE);

        // доступ только админу
        String role = prefs.getString("role", null);
        if (!"admin".equals(role)) {
            Toast.makeText(this, "Доступ только для администратора", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        adminId = prefs.getInt("userId", -1);
        if (adminId == -1) {
            Toast.makeText(this, "Нет сессии администратора", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // views
        spEmployee = findViewById(R.id.spEmployee);

        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etPhone = findViewById(R.id.etPhone);
        etRate = findViewById(R.id.etRate);
        etHours = findViewById(R.id.etHoursPerShift);

        spPosition = findViewById(R.id.spPosition);

        btnTogglePassword = findViewById(R.id.btnTogglePassword);

        btnSave = findViewById(R.id.btnSaveEmployee);
        btnDelete = findViewById(R.id.btnDeleteEmployee);

        // старт: пароль скрыт
        setPasswordVisible(false);
        btnTogglePassword.setOnClickListener(v -> setPasswordVisible(!isPasswordVisible));

        // должности
        String[] positions = {"Повар", "Кассир", "Менеджер", "Курьер", "Администратор"};
        ArrayAdapter<String> posAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, positions);
        posAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spPosition.setAdapter(posAdapter);

        btnSave.setOnClickListener(v -> saveChanges());
        btnDelete.setOnClickListener(v -> confirmDelete());

        loadEmployees();
    }

    private void setPasswordVisible(boolean visible) {
        isPasswordVisible = visible;

        if (visible) {
            etPassword.setTransformationMethod(null);
            btnTogglePassword.setText("🙈");
        } else {
            etPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            btnTogglePassword.setText("👁");
        }

        // курсор в конец
        if (etPassword.getText() != null) {
            etPassword.setSelection(etPassword.getText().length());
        }
    }

    private void loadEmployees() {
        staff = db.userDao().getEmployeesForAdmin(adminId);

        // чтобы не было повторных вызовов
        spEmployee.setOnItemSelectedListener(null);

        if (staff == null || staff.isEmpty()) {
            Toast.makeText(this, "Сотрудников нет. Добавьте сотрудников.", Toast.LENGTH_SHORT).show();
            spEmployee.setAdapter(null);
            currentUser = null;

            setFieldsEnabled(false);
            btnSave.setEnabled(false);
            btnDelete.setEnabled(false);

            return;
        }

        List<String> labels = new ArrayList<>();
        for (User u : staff) {
            String name = (u.lastName == null ? "" : u.lastName + " ") + (u.firstName == null ? "" : u.firstName);
            String pos = (u.position == null ? "" : " (" + u.position + ")");
            labels.add(name.trim() + pos);
        }

        ArrayAdapter<String> empAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, labels);
        empAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spEmployee.setAdapter(empAdapter);

        setFieldsEnabled(true);
        btnSave.setEnabled(true);
        btnDelete.setEnabled(true);

        spEmployee.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                currentUser = staff.get(position);
                fillFields(currentUser);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        // показать первого
        spEmployee.setSelection(0);
        currentUser = staff.get(0);
        fillFields(currentUser);
    }

    private void fillFields(User u) {
        if (u == null) return;

        etFirstName.setText(u.firstName == null ? "" : u.firstName);
        etLastName.setText(u.lastName == null ? "" : u.lastName);
        etEmail.setText(u.email == null ? "" : u.email);
        etPassword.setText(u.password == null ? "" : u.password);
        etPhone.setText(u.phone == null ? "" : u.phone);

        etRate.setText(String.valueOf(u.ratePerHour));
        etHours.setText(String.valueOf(u.hoursPerShift));

        // должность
        String pos = u.position == null ? "" : u.position;
        for (int i = 0; i < spPosition.getCount(); i++) {
            if (pos.equals(spPosition.getItemAtPosition(i))) {
                spPosition.setSelection(i);
                break;
            }
        }

        // когда переключаем сотрудника — снова скрываем пароль
        setPasswordVisible(false);
    }

    private void setFieldsEnabled(boolean enabled) {
        etFirstName.setEnabled(enabled);
        etLastName.setEnabled(enabled);
        etEmail.setEnabled(enabled);
        etPassword.setEnabled(enabled);
        etPhone.setEnabled(enabled);
        etRate.setEnabled(enabled);
        etHours.setEnabled(enabled);
        spPosition.setEnabled(enabled);
        btnTogglePassword.setEnabled(enabled);
    }

    private void saveChanges() {
        if (currentUser == null) return;

        String firstName = etFirstName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();
        String phone = etPhone.getText().toString().trim();
        String position = (String) spPosition.getSelectedItem();

        String rateStr = etRate.getText().toString().trim();
        String hoursStr = etHours.getText().toString().trim();

        // обязательные поля
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Некорректный e-mail");
            return;
        }
        if (password == null || password.length() < 4) {
            etPassword.setError("Минимум 4 символа");
            return;
        }
        if (phone.isEmpty()) {
            etPhone.setError("Введите телефон");
            return;
        }

        // числа
        double rate;
        int hours;
        try {
            rate = Double.parseDouble(rateStr);
            hours = Integer.parseInt(hoursStr);
        } catch (Exception e) {
            Toast.makeText(this, "Проверьте ставку и часы", Toast.LENGTH_SHORT).show();
            return;
        }

        // уникальность email
        User conflict = db.userDao().getByEmailExceptId(email, currentUser.id);
        if (conflict != null) {
            etEmail.setError("Этот e-mail уже занят");
            return;
        }

        // обновляем
        currentUser.firstName = firstName;
        currentUser.lastName = lastName;
        currentUser.email = email;
        currentUser.password = password;
        currentUser.phone = phone;
        currentUser.position = position;
        currentUser.ratePerHour = rate;
        currentUser.hoursPerShift = hours;

        db.userDao().update(currentUser);

        Toast.makeText(this, "Сохранено", Toast.LENGTH_SHORT).show();

        // обновить подписи в spinner и сохранить выбранного
        int keepId = currentUser.id;
        loadEmployees();
        restoreSelectionById(keepId);
    }

    private void restoreSelectionById(int userId) {
        if (staff == null || staff.isEmpty()) return;

        for (int i = 0; i < staff.size(); i++) {
            if (staff.get(i).id == userId) {
                spEmployee.setSelection(i);
                currentUser = staff.get(i);
                fillFields(currentUser);
                return;
            }
        }
    }

    private void confirmDelete() {
        if (currentUser == null) return;

        String fullName =
                (currentUser.lastName == null ? "" : currentUser.lastName + " ") +
                        (currentUser.firstName == null ? "" : currentUser.firstName);

        new AlertDialog.Builder(this)
                .setTitle("Удалить сотрудника?")
                .setMessage("Вы уверены, что хотите удалить: " + fullName.trim() +
                        "?\n\nВсе его графики будут удалены.")
                .setPositiveButton("Удалить", (d, which) -> deleteCurrent())
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void deleteCurrent() {
        if (currentUser == null) return;

        int deleteId = currentUser.id;

        db.userDao().deleteById(deleteId);

        Toast.makeText(this, "Сотрудник удалён", Toast.LENGTH_SHORT).show();

        // сбросить выбранных, чтобы не держать битый id
        int selectedHome = prefs.getInt("selectedEmployeeId", -1);
        if (selectedHome == deleteId) {
            prefs.edit().remove("selectedEmployeeId").apply();
        }
        int selectedStat = prefs.getInt("selectedEmployeeIdStat", -1);
        if (selectedStat == deleteId) {
            prefs.edit().remove("selectedEmployeeIdStat").apply();
        }

        currentUser = null;
        loadEmployees();
    }
}
