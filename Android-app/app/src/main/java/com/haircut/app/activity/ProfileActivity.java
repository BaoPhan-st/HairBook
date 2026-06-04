package com.haircut.app.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.haircut.app.R;
import com.haircut.app.api.ApiClient;
import com.haircut.app.api.ApiService;
import com.haircut.app.model.UserModel;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class ProfileActivity extends AppCompatActivity {

    private TextView tvFullName, tvEmail, tvNameValue, tvEmailValue, tvPhoneValue;
    private Button btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        initViews();
        loadProfile();
    }

    private void initViews() {
        tvFullName   = findViewById(R.id.tv_full_name);
        tvEmail      = findViewById(R.id.tv_email);
        tvNameValue  = findViewById(R.id.tv_name_value);
        tvEmailValue = findViewById(R.id.tv_email_value);
        tvPhoneValue = findViewById(R.id.tv_phone_value);
        btnLogout    = findViewById(R.id.btn_logout);

        btnLogout.setOnClickListener(v -> logout());
    }

    private void loadProfile() {
        ApiClient.getService(this).getCurrentUser().enqueue(new Callback<UserModel>() {
            @Override
            public void onResponse(Call<UserModel> call, Response<UserModel> response) {
                if (response.isSuccessful() && response.body() != null) {
                    UserModel u = response.body();
                    tvFullName.setText(u.fullName != null ? u.fullName : "");
                    tvEmail.setText(u.email != null ? u.email : "");
                    tvNameValue.setText(u.fullName != null ? u.fullName : "—");
                    tvEmailValue.setText(u.email != null ? u.email : "—");
                    tvPhoneValue.setText(u.phone != null ? u.phone : "—");
                }
            }
            @Override
            public void onFailure(Call<UserModel> call, Throwable t) {
                Toast.makeText(ProfileActivity.this, "Lỗi tải thông tin", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void logout() {
        ApiClient.clearToken(this);
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
