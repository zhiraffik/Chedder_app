package com.example.chedder_1.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.chedder_1.MainActivity;
import com.example.chedder_1.R;
import com.example.chedder_1.domain.db.AppDataBase;
import com.example.chedder_1.domain.entity.User;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;

    private SharedPreferences prefs;
    private AppDataBase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences("auth", MODE_PRIVATE);

        // если уже авторизован — сразу в MainActivity
        if (prefs.contains("userId")) {
            openMain();
            return;
        }

        setContentView(R.layout.activity_login);

        // инициализация БД
        db = AppDataBase.getInstance(this);

        // создаём администратора, если БД пустая
        createAdminIfNotExists();

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(v -> login());
    }

    private void login() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();

        // валидация
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Некорректный e-mail");
            return;
        }

        if (password.length() < 4) {
            etPassword.setError("Минимум 4 символа");
            return;
        }

        // 🔐 АВТОРИЗАЦИЯ ЧЕРЕЗ ROOM
        User user = db.userDao().login(email, password);

        if (user != null) {
            prefs.edit()
                    .putInt("userId", user.id)
                    .putString("role", user.role)
                    .apply();

            openMain();
        } else {
            Toast.makeText(this, "Неверный логин или пароль", Toast.LENGTH_SHORT).show();
        }
    }

    private void createAdminIfNotExists() {
        if (db.userDao().getAll().isEmpty()) {
            User admin = new User();
            admin.email = "admin@mail.ru";
            admin.password = "admin";
            admin.role = "admin";
            admin.firstName = "Администратор";
            admin.lastName = "";
            admin.position = "Админ";
            admin.ratePerHour = 0;
            admin.hoursPerShift = 0;

            db.userDao().insert(admin);
        }
    }

    private void openMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
