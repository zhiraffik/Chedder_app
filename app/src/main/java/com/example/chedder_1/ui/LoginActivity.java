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
        db = AppDataBase.getInstance(this);

        // Создаём администратора, если БД пустая (или админа нет)
        createAdminIfNotExists();

        // Если уже авторизован — проверим, что пользователь реально существует
        if (hasValidSession()) {
            openMain();
            return;
        } else {
            // если в prefs что-то осталось битое — почистим
            prefs.edit().clear().apply();
        }

        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(v -> login());
    }

    private boolean hasValidSession() {
        if (!prefs.contains("userId")) return false;

        int userId = prefs.getInt("userId", -1);
        if (userId == -1) return false;

        User u = db.userDao().getById(userId);
        return u != null;
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

        // 🔐 Авторизация через Room
        User user = db.userDao().login(email, password);

        if (user != null) {
            // Для employee важно знать adminId (чтобы грузить его график)
            int adminIdForSession;
            if ("admin".equals(user.role)) {
                adminIdForSession = user.id; // админ сам себе владелец
            } else {
                adminIdForSession = (user.adminId != null) ? user.adminId : -1;
            }

            prefs.edit()
                    .putInt("userId", user.id)
                    .putString("role", user.role)
                    .putInt("adminId", adminIdForSession)
                    .apply();

            openMain();
        } else {
            Toast.makeText(this, "Неверный логин или пароль", Toast.LENGTH_SHORT).show();
        }
    }

    private void createAdminIfNotExists() {

        User anyAdmin = db.userDao().getAnyAdmin();
        if (anyAdmin != null) return;

        User admin = new User();
        admin.email = "admin@mail.ru";
        admin.password = "admin";
        admin.role = "admin";
        admin.firstName = "Администратор";
        admin.lastName = "";
        admin.position = "admin";
        admin.ratePerHour = 0;
        admin.hoursPerShift = 0;
        admin.adminId = null;

        db.userDao().insert(admin);
    }

    private void openMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
