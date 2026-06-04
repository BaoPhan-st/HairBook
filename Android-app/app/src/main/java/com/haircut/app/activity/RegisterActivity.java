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
import com.haircut.app.api.ApiClient;
import com.haircut.app.api.ApiService;
import com.haircut.app.model.AuthResponse;
import com.haircut.app.model.RegisterRequest;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class RegisterActivity extends AppCompatActivity {

    private EditText etFullName, etEmail, etPhone, etPassword, etConfirmPassword;
    private Button btnRegister;
    private TextView tvLogin;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        initViews();
        setupListeners();
    }

    private void initViews() {
        etFullName        = findViewById(R.id.et_full_name);
        etEmail           = findViewById(R.id.et_email);
        etPhone           = findViewById(R.id.et_phone);
        etPassword        = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        btnRegister       = findViewById(R.id.btn_register);
        tvLogin           = findViewById(R.id.tv_login);
        progressBar       = findViewById(R.id.progress_bar);
    }

    private void setupListeners() {
        btnRegister.setOnClickListener(v -> attemptRegister());
        tvLogin.setOnClickListener(v -> finish()); // Back to LoginActivity
    }

    private void attemptRegister() {
        String fullName = etFullName.getText().toString().trim();
        String email    = etEmail.getText().toString().trim();
        String phone    = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirm  = etConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(fullName)) { etFullName.setError("Vui lòng nhập họ tên"); return; }
        if (TextUtils.isEmpty(email))    { etEmail.setError("Vui lòng nhập email"); return; }
        if (TextUtils.isEmpty(phone))    { etPhone.setError("Vui lòng nhập số điện thoại"); return; }
        if (password.length() < 6)       { etPassword.setError("Mật khẩu ít nhất 6 ký tự"); return; }
        if (!password.equals(confirm))   { etConfirmPassword.setError("Mật khẩu không khớp"); return; }

        setLoading(true);

        RegisterRequest req = new RegisterRequest();
        req.fullName = fullName;
        req.email    = email;
        req.phone    = phone;
        req.password = password;

        ApiClient.getService(this).register(req).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    ApiClient.saveToken(RegisterActivity.this, response.body().token);
                    Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                } else {
                    Toast.makeText(RegisterActivity.this,
                        "Email đã tồn tại hoặc thông tin không hợp lệ", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                setLoading(false);
                Toast.makeText(RegisterActivity.this,
                    "Không kết nối được server!", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!loading);
        btnRegister.setText(loading ? "Đang đăng ký..." : "Đăng ký");
    }
}
