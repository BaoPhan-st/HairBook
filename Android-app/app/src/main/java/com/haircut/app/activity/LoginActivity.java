package com.haircut.app.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.haircut.app.R;
import com.haircut.app.admin.AdminDashboardActivity;
import com.haircut.app.api.ApiClient;
import com.haircut.app.api.ApiService;
import com.haircut.app.model.AuthResponse;
import com.haircut.app.model.LoginRequest;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvRegister;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Nếu đã đăng nhập, điều hướng theo role
        if (ApiClient.isLoggedIn(this)) {
            navigateByRole();
            return;
        }

        setContentView(R.layout.activity_login);
        initViews();
        setupListeners();
    }

    private void initViews() {
        etEmail     = findViewById(R.id.et_email);
        etPassword  = findViewById(R.id.et_password);
        btnLogin    = findViewById(R.id.btn_login);
        tvRegister  = findViewById(R.id.tv_register);
        progressBar = findViewById(R.id.progress_bar);
    }

    private void setupListeners() {
        btnLogin.setOnClickListener(v -> attemptLogin());
        tvRegister.setOnClickListener(v ->
            startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void attemptLogin() {
        String email    = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email))    { etEmail.setError("Vui lòng nhập email"); return; }
        if (TextUtils.isEmpty(password)) { etPassword.setError("Vui lòng nhập mật khẩu"); return; }

        setLoading(true);
        ApiClient.getService(this).login(new LoginRequest(email, password))
            .enqueue(new Callback<AuthResponse>() {
                @Override
                public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                    setLoading(false);
                    if (response.isSuccessful() && response.body() != null) {
                        AuthResponse body = response.body();
                        // Lưu token + thông tin user (role, fullName, email)
                        String role = body.role != null ? body.role : "CUSTOMER";
                        ApiClient.saveUserInfo(LoginActivity.this,
                                body.token, role, body.fullName, body.email);
                        navigateByRole();
                    } else if (response.code() == 403) {
                        Toast.makeText(LoginActivity.this,
                            "Tài khoản của bạn đã bị khoá. Liên hệ quản trị viên!",
                            Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(LoginActivity.this,
                            "Email hoặc mật khẩu không đúng", Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onFailure(Call<AuthResponse> call, Throwable t) {
                    setLoading(false);
                    Toast.makeText(LoginActivity.this,
                        "Không kết nối được server. Kiểm tra lại mạng!", Toast.LENGTH_LONG).show();
                }
            });
    }

    /** Điều hướng đến màn hình tương ứng với role */
    private void navigateByRole() {
        Intent intent;
        if (ApiClient.isAdmin(this)) {
            intent = new Intent(this, AdminDashboardActivity.class);
        } else {
            intent = new Intent(this, MainActivity.class);
        }
        startActivity(intent);
        finish();
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!loading);
        btnLogin.setText(loading ? "Đang đăng nhập..." : "Đăng nhập");
    }
}
