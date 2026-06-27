package com.haircut.app.admin;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.haircut.app.R;
import com.haircut.app.api.ApiClient;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminUserDetailActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private View layoutContent;

    private TextView tvInitial, tvFullName, tvEmail, tvPhone;
    private TextView tvRole, tvStatus, tvCreatedAt;
    private Button btnChangeRole, btnToggleStatus;

    private AdminUserModel currentUser;
    private Long userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_user_detail);

        userId = getIntent().getLongExtra("userId", -1L);
        if (userId == -1L) { finish(); return; }

        initViews();
        loadUserDetail();
    }

    private void initViews() {
        progressBar   = findViewById(R.id.progress_bar);
        layoutContent = findViewById(R.id.layout_content);

        tvInitial   = findViewById(R.id.tv_initial);
        tvFullName  = findViewById(R.id.tv_full_name);
        tvEmail     = findViewById(R.id.tv_email);
        tvPhone     = findViewById(R.id.tv_phone);
        tvRole      = findViewById(R.id.tv_role);
        tvStatus    = findViewById(R.id.tv_status);
        tvCreatedAt = findViewById(R.id.tv_created_at);
        btnChangeRole   = findViewById(R.id.btn_change_role);
        btnToggleStatus = findViewById(R.id.btn_toggle_status);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        btnChangeRole.setOnClickListener(v -> confirmChangeRole());
        btnToggleStatus.setOnClickListener(v -> confirmToggleStatus());
    }

    private void loadUserDetail() {
        setLoading(true);
        ApiClient.getService(this).getUserDetail(userId)
            .enqueue(new Callback<AdminUserModel>() {
                @Override
                public void onResponse(Call<AdminUserModel> call, Response<AdminUserModel> response) {
                    setLoading(false);
                    if (response.isSuccessful() && response.body() != null) {
                        currentUser = response.body();
                        bindUser(currentUser);
                    } else {
                        Toast.makeText(AdminUserDetailActivity.this,
                            "Không tìm thấy người dùng", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }

                @Override
                public void onFailure(Call<AdminUserModel> call, Throwable t) {
                    setLoading(false);
                    Toast.makeText(AdminUserDetailActivity.this,
                        "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
    }

    private void bindUser(AdminUserModel user) {
        tvInitial.setText(user.getInitial());
        tvFullName.setText(user.fullName != null ? user.fullName : "-");
        tvEmail.setText(user.email != null ? user.email : "-");
        tvPhone.setText((user.phone != null && !user.phone.isEmpty()) ? user.phone : "Chưa cập nhật");
        tvRole.setText(user.isAdmin() ? "🛡 Quản trị viên (ADMIN)" : "👤 Khách hàng (CUSTOMER)");
        tvStatus.setText(user.isLocked() ? "🔒 Bị khoá" : "✅ Đang hoạt động");
        tvCreatedAt.setText("Tạo lúc: " + (user.createdAt != null ? user.createdAt : "-"));

        btnChangeRole.setText(user.isAdmin() ? "Hạ xuống CUSTOMER" : "Nâng lên ADMIN");
        btnToggleStatus.setText(user.isLocked() ? "Mở khoá tài khoản" : "Khoá tài khoản");

        int statusColor = user.isLocked()
            ? getResources().getColor(R.color.status_cancelled, null)
            : getResources().getColor(R.color.status_confirmed, null);
        tvStatus.setTextColor(statusColor);
    }

    private void confirmChangeRole() {
        if (currentUser == null) return;
        String newRole   = currentUser.isAdmin() ? "CUSTOMER" : "ADMIN";
        String newRoleVn = currentUser.isAdmin() ? "Khách hàng (CUSTOMER)" : "Quản trị viên (ADMIN)";

        new AlertDialog.Builder(this)
            .setTitle("Thay đổi quyền")
            .setMessage("Bạn có chắc muốn đổi quyền của \""
                    + currentUser.fullName + "\" thành " + newRoleVn + "?")
            .setPositiveButton("Xác nhận", (dialog, which) -> updateRole(newRole))
            .setNegativeButton("Huỷ", null)
            .show();
    }

    private void confirmToggleStatus() {
        if (currentUser == null) return;
        String newStatus = currentUser.isLocked() ? "ACTIVE" : "LOCKED";
        String actionVn  = currentUser.isLocked() ? "mở khoá" : "khoá";

        new AlertDialog.Builder(this)
            .setTitle(currentUser.isLocked() ? "Mở khoá tài khoản" : "Khoá tài khoản")
            .setMessage("Bạn có chắc muốn " + actionVn + " tài khoản của \""
                    + currentUser.fullName + "\"?")
            .setPositiveButton("Xác nhận", (dialog, which) -> updateStatus(newStatus))
            .setNegativeButton("Huỷ", null)
            .show();
    }

    private void updateRole(String newRole) {
        Map<String, String> body = new HashMap<>();
        body.put("role", newRole);

        setLoading(true);
        ApiClient.getService(this).updateUserRole(userId, body)
            .enqueue(new Callback<AdminUserModel>() {
                @Override
                public void onResponse(Call<AdminUserModel> call, Response<AdminUserModel> response) {
                    setLoading(false);
                    if (response.isSuccessful() && response.body() != null) {
                        currentUser = response.body();
                        bindUser(currentUser);
                        Toast.makeText(AdminUserDetailActivity.this,
                            "Đã cập nhật quyền thành công!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(AdminUserDetailActivity.this,
                            "Cập nhật quyền thất bại", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<AdminUserModel> call, Throwable t) {
                    setLoading(false);
                    Toast.makeText(AdminUserDetailActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void updateStatus(String newStatus) {
        Map<String, String> body = new HashMap<>();
        body.put("status", newStatus);

        setLoading(true);
        ApiClient.getService(this).updateUserStatus(userId, body)
            .enqueue(new Callback<AdminUserModel>() {
                @Override
                public void onResponse(Call<AdminUserModel> call, Response<AdminUserModel> response) {
                    setLoading(false);
                    if (response.isSuccessful() && response.body() != null) {
                        currentUser = response.body();
                        bindUser(currentUser);
                        String msg = "LOCKED".equals(newStatus) ? "Đã khoá tài khoản!" : "Đã mở khoá tài khoản!";
                        Toast.makeText(AdminUserDetailActivity.this, msg, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(AdminUserDetailActivity.this,
                            "Cập nhật trạng thái thất bại", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<AdminUserModel> call, Throwable t) {
                    setLoading(false);
                    Toast.makeText(AdminUserDetailActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        layoutContent.setVisibility(loading ? View.INVISIBLE : View.VISIBLE);
    }
}
