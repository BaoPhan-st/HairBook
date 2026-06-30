package com.haircut.app.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.haircut.app.R;
import com.haircut.app.activity.BarberManagementActivity;
import com.haircut.app.activity.LoginActivity;
import com.haircut.app.activity.MainActivity;
import com.haircut.app.api.ApiClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminDashboardActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private View layoutContent;

    private TextView tvAdminName;
    private TextView tvTotalUsers, tvTotalBookings, tvPendingBookings, tvTodayBookings;
    private TextView tvTotalCustomers, tvTotalAdmins;
    private TextView tvConfirmedBookings, tvCompletedBookings, tvCancelledBookings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Kiểm tra quyền admin
        if (!ApiClient.isAdmin(this)) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_admin_dashboard);
        initViews();
        loadDashboard();
    }

    private void initViews() {
        progressBar   = findViewById(R.id.progress_bar);
        layoutContent = findViewById(R.id.layout_content);

        tvAdminName         = findViewById(R.id.tv_admin_name);
        tvTotalUsers        = findViewById(R.id.tv_total_users);
        tvTotalBookings     = findViewById(R.id.tv_total_bookings);
        tvPendingBookings   = findViewById(R.id.tv_pending_bookings);
        tvTodayBookings     = findViewById(R.id.tv_today_bookings);
        tvTotalCustomers    = findViewById(R.id.tv_total_customers);
        tvTotalAdmins       = findViewById(R.id.tv_total_admins);
        tvConfirmedBookings = findViewById(R.id.tv_confirmed_bookings);
        tvCompletedBookings = findViewById(R.id.tv_completed_bookings);
        tvCancelledBookings = findViewById(R.id.tv_cancelled_bookings);

        // Tên admin từ SharedPreferences
        String adminName = ApiClient.getUserName(this);
        tvAdminName.setText("Xin chào, " + (adminName.isEmpty() ? "Admin" : adminName));

        // Nút Quản lý Users
        findViewById(R.id.btn_manage_users).setOnClickListener(v ->
            startActivity(new Intent(this, AdminUsersActivity.class)));

        findViewById(R.id.btn_manage_barbers).setOnClickListener(v ->
                startActivity(new Intent(this, BarberManagementActivity.class)));

        // Nút Logout
        findViewById(R.id.btn_logout).setOnClickListener(v -> confirmLogout());
    }

    private void loadDashboard() {
        setLoading(true);
        ApiClient.getService(this).getDashboardStats()
            .enqueue(new Callback<DashboardStatsModel>() {
                @Override
                public void onResponse(Call<DashboardStatsModel> call, Response<DashboardStatsModel> response) {
                    setLoading(false);
                    if (response.isSuccessful() && response.body() != null) {
                        bindStats(response.body());
                    } else if (response.code() == 401 || response.code() == 403) {
                        handleUnauthorized();
                    } else {
                        Toast.makeText(AdminDashboardActivity.this,
                            "Không thể tải dữ liệu dashboard", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<DashboardStatsModel> call, Throwable t) {
                    setLoading(false);
                    Toast.makeText(AdminDashboardActivity.this,
                        "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void bindStats(DashboardStatsModel stats) {
        tvTotalUsers.setText(String.valueOf(stats.totalUsers));
        tvTotalBookings.setText(String.valueOf(stats.totalBookings));
        tvPendingBookings.setText(String.valueOf(stats.pendingBookings));
        tvTodayBookings.setText(String.valueOf(stats.todayBookings));
        tvTotalCustomers.setText("Khách hàng: " + stats.totalCustomers);
        tvTotalAdmins.setText("Quản trị: " + stats.totalAdmins);
        tvConfirmedBookings.setText("Đã xác nhận: " + stats.confirmedBookings);
        tvCompletedBookings.setText("Hoàn thành: " + stats.completedBookings);
        tvCancelledBookings.setText("Đã huỷ: " + stats.cancelledBookings);
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this)
            .setTitle("Đăng xuất")
            .setMessage("Bạn có chắc muốn đăng xuất không?")
            .setPositiveButton("Đăng xuất", (dialog, which) -> logout())
            .setNegativeButton("Huỷ", null)
            .show();
    }

    private void logout() {
        ApiClient.clearAll(this);
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void handleUnauthorized() {
        ApiClient.clearAll(this);
        Toast.makeText(this, "Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại.", Toast.LENGTH_LONG).show();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        layoutContent.setVisibility(loading ? View.GONE : View.VISIBLE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDashboard();
    }
}
