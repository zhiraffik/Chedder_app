package com.example.chedder_1.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.chedder_1.R;
import com.example.chedder_1.domain.db.AppDataBase;
import com.example.chedder_1.domain.entity.User;

public class AddEmployeeActivity extends AppCompatActivity {

    private EditText etFirstName, etLastName, etEmail, etPassword, etPhone, etRate, etHours;
    private Spinner spPosition;
    private Button btnSave;

    private AppDataBase db;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_employee);

        db = AppDataBase.getInstance(this);
        prefs = getSharedPreferences("auth", Context.MODE_PRIVATE);

        String role = prefs.getString("role", null);
        if (!"admin".equals(role)) {
            Toast.makeText(this, "Доступ только для администратора", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        etFirstName = findViewById(R.id.etFirstName);
        etLastName  = findViewById(R.id.etLastName);
        etEmail     = findViewById(R.id.etEmail);
        etPassword  = findViewById(R.id.etPassword);
        etRate      = findViewById(R.id.etRate);
        etHours     = findViewById(R.id.etHoursPerShift);
        spPosition  = findViewById(R.id.spPosition);
        btnSave     = findViewById(R.id.btnSaveEmployee);

        String[] positions = {"Повар", "Кассир", "Менеджер", "Курьер"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, positions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spPosition.setAdapter(adapter);

        btnSave.setOnClickListener(v -> saveEmployee());
    }

    private void saveEmployee() {
        int adminId = prefs.getInt("userId", -1);
        if (adminId == -1) {
            Toast.makeText(this, "Нет сессии администратора", Toast.LENGTH_SHORT).show();
            return;
        }

        String firstName = etFirstName.getText().toString().trim();
        String lastName  = etLastName.getText().toString().trim();
        String email     = etEmail.getText().toString().trim();
        String password  = etPassword.getText().toString();
        String position  = (String) spPosition.getSelectedItem();

        String rateStr   = etRate.getText().toString().trim();
        String hoursStr  = etHours.getText().toString().trim();

        if (firstName.isEmpty()) { etFirstName.setError("Введите имя"); return; }

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Некорректный e-mail");
            return;
        }

        if (password.length() < 4) { etPassword.setError("Минимум 4 символа"); return; }


        if (rateStr.isEmpty()) { etRate.setError("Введите ставку"); return; }
        if (hoursStr.isEmpty()) { etHours.setError("Введите часы"); return; }

        double rate;
        int hours;
        try {
            rate = Double.parseDouble(rateStr);
            hours = Integer.parseInt(hoursStr);
        } catch (Exception e) {
            Toast.makeText(this, "Проверьте числа", Toast.LENGTH_SHORT).show();
            return;
        }

        User existing = db.userDao().getByEmail(email);
        if (existing != null) {
            etEmail.setError("Такой e-mail уже существует");
            return;
        }

        User emp = new User();
        emp.email = email;
        emp.password = password;

        emp.role = "employee";
        emp.firstName = firstName;
        emp.lastName = lastName;
        emp.position = position;
        emp.ratePerHour = rate;
        emp.hoursPerShift = hours;
        emp.adminId = adminId;

        db.userDao().insert(emp);

        Toast.makeText(this, "Сотрудник добавлен", Toast.LENGTH_SHORT).show();
        finish();
    }
}
