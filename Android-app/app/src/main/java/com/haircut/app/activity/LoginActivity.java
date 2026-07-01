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

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.haircut.app.BuildConfig;
import com.haircut.app.R;
import com.haircut.app.api.ApiClient;
import com.haircut.app.api.ApiService;
import com.haircut.app.model.AuthResponse;
import com.haircut.app.model.GoogleLoginRequest;
import com.haircut.app.model.LoginRequest;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin, btnGoogleLogin;
    private TextView tvRegister;
    private ProgressBar progressBar;

    private GoogleSignInClient googleSignInClient;

    private final ActivityResultLauncher<Intent> googleSignInLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        Task<GoogleSignInAccount> task =
                                GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                        handleGoogleSignInResult(task);
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (ApiClient.isLoggedIn(this)) { goToMain(); return; }

        setContentView(R.layout.activity_login);
        initViews();
        setupGoogleSignIn();
        setupListeners();
    }

    private void initViews() {
        etEmail       = findViewById(R.id.et_email);
        etPassword    = findViewById(R.id.et_password);
        btnLogin      = findViewById(R.id.btn_login);
        btnGoogleLogin = findViewById(R.id.btn_google_login);
        tvRegister    = findViewById(R.id.tv_register);
        progressBar   = findViewById(R.id.progress_bar);
    }

    private void setupGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                .requestEmail()
                .requestProfile()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void setupListeners() {
        btnLogin.setOnClickListener(v -> attemptLogin());
        btnGoogleLogin.setOnClickListener(v -> startGoogleSignIn());
        tvRegister.setOnClickListener(v ->
            startActivity(new Intent(this, RegisterActivity.class)));
    }

    // ---- Email/Password login ----
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
                        ApiClient.saveToken(LoginActivity.this, response.body().token);
                        goToMain();
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

    // ---- Google Sign-In ----
    private void startGoogleSignIn() {
        // Luôn hiển thị hộp thoại chọn tài khoản
        googleSignInClient.signOut().addOnCompleteListener(this, task -> {
            Intent signInIntent = googleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });
    }

    private void handleGoogleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            String idToken = account.getIdToken();
            if (idToken == null) {
                Toast.makeText(this, "Không lấy được Google Token. Thử lại!", Toast.LENGTH_SHORT).show();
                return;
            }
            sendGoogleTokenToBackend(idToken);
        } catch (ApiException e) {
            Toast.makeText(this,
                "Đăng nhập Google thất bại (mã " + e.getStatusCode() + ")",
                Toast.LENGTH_SHORT).show();
        }
    }

    private void sendGoogleTokenToBackend(String idToken) {
        setLoading(true);
        ApiClient.getService(this).googleLogin(new GoogleLoginRequest(idToken))
            .enqueue(new Callback<AuthResponse>() {
                @Override
                public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                    setLoading(false);
                    if (response.isSuccessful() && response.body() != null) {
                        ApiClient.saveToken(LoginActivity.this, response.body().token);
                        Toast.makeText(LoginActivity.this,
                            "Chào mừng " + response.body().fullName + "!",
                            Toast.LENGTH_SHORT).show();
                        goToMain();
                    } else {
                        Toast.makeText(LoginActivity.this,
                            "Đăng nhập Google thất bại. Thử lại!", Toast.LENGTH_SHORT).show();
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

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!loading);
        btnGoogleLogin.setEnabled(!loading);
        btnLogin.setText(loading ? "Đang đăng nhập..." : "Đăng nhập");
    }

    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}

