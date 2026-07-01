package com.haircut.app.admin;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import com.haircut.app.R;
import com.haircut.app.api.ApiClient;
import com.haircut.app.api.ApiService;
import com.haircut.app.model.BookingModel;
import com.haircut.app.model.CancelRequest;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminBookingActivity extends AppCompatActivity {

    private RecyclerView rvBookings;
    private ProgressBar progressBar;
    private TextView tvEmpty, tvPendingBadge;
    private TabLayout tabLayout;
    private Button btnPickDate, btnClearDate;
    private AdminBookingAdapter adapter;
    private ApiService apiService;

    private List<BookingModel> allBookings = new ArrayList<>();
    private String selectedDate = null; // null = không lọc ngày, "yyyy-MM-dd" = lọc theo ngày

    // Tab: 0 Tất cả | 1 Chờ xác nhận | 2 Đã xác nhận | 3 Đang thực hiện | 4 Hoàn thành | 5 Đã huỷ
    private static final String[] TAB_STATUSES = {null, "PENDING", "CONFIRMED", "IN_PROGRESS", "COMPLETED", null};
    private static final int TAB_CANCELLED = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_bookings);
        apiService = ApiClient.getService(this);

        initViews();
        loadBookings();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadBookings();
    }

    private void initViews() {
        rvBookings      = findViewById(R.id.rv_bookings);
        progressBar     = findViewById(R.id.progress_bar);
        tvEmpty         = findViewById(R.id.tv_empty);
        tvPendingBadge  = findViewById(R.id.tv_pending_badge);
        tabLayout       = findViewById(R.id.tab_layout);
        btnPickDate     = findViewById(R.id.btn_pick_date);
        btnClearDate    = findViewById(R.id.btn_clear_date);

        rvBookings.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdminBookingAdapter(new ArrayList<>(), new AdminBookingAdapter.ActionListener() {
            @Override public void onConfirm(BookingModel b)  { confirmDialog(b); }
            @Override public void onReject(BookingModel b)   { rejectDialog(b); }
            @Override public void onStart(BookingModel b)    { startDialog(b); }
            @Override public void onComplete(BookingModel b) { completeDialog(b); }
            @Override public void onNoShow(BookingModel b)   { noShowDialog(b); }
        });
        rvBookings.setAdapter(adapter);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) { applyFilter(); }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        btnPickDate.setOnClickListener(v -> showDatePicker());
        btnClearDate.setOnClickListener(v -> clearDateFilter());
    }

    // ── Load dữ liệu ─────────────────────────────────────────────────────────

    private void loadBookings() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        apiService.getAllAdminBookings(null, null)
                .enqueue(new Callback<List<BookingModel>>() {
                    @Override
                    public void onResponse(Call<List<BookingModel>> call, Response<List<BookingModel>> resp) {
                        progressBar.setVisibility(View.GONE);
                        if (resp.isSuccessful() && resp.body() != null) {
                            allBookings = resp.body();
                            updatePendingBadge();
                            applyFilter();
                        } else {
                            showEmpty(true);
                            Toast.makeText(AdminBookingActivity.this,
                                    parseError(resp), Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    public void onFailure(Call<List<BookingModel>> call, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        showEmpty(true);
                        Toast.makeText(AdminBookingActivity.this,
                                "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ── Filter theo tab + ngày ────────────────────────────────────────────────

    private void applyFilter() {
        int pos = tabLayout.getSelectedTabPosition();
        List<BookingModel> filtered = allBookings.stream()
                .filter(b -> {
                    // Filter theo tab
                    if (pos == TAB_CANCELLED) {
                        boolean cancelled = "CANCELLED_BY_CUSTOMER".equals(b.status)
                                || "CANCELLED_BY_SALON".equals(b.status)
                                || "NO_SHOW".equals(b.status);
                        if (!cancelled) return false;
                    } else if (TAB_STATUSES[pos] != null) {
                        if (!TAB_STATUSES[pos].equals(b.status)) return false;
                    }
                    // Filter theo ngày
                    if (selectedDate != null && b.bookingTime != null) {
                        return b.bookingTime.startsWith(selectedDate);
                    }
                    return true;
                })
                .collect(Collectors.toList());

        adapter.updateData(filtered);
        showEmpty(filtered.isEmpty());
    }

    private void updatePendingBadge() {
        long count = allBookings.stream()
                .filter(b -> "PENDING".equals(b.status)).count();
        if (count > 0) {
            tvPendingBadge.setText(count + " chờ duyệt");
            tvPendingBadge.setVisibility(View.VISIBLE);
        } else {
            tvPendingBadge.setVisibility(View.GONE);
        }
    }

    // ── Chọn ngày lọc ────────────────────────────────────────────────────────

    private void showDatePicker() {
        Calendar now = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) -> {
            selectedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, day);
            btnPickDate.setText(day + "/" + (month + 1) + "/" + year);
            btnClearDate.setVisibility(View.VISIBLE);
            applyFilter();
        }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void clearDateFilter() {
        selectedDate = null;
        btnPickDate.setText("Tất cả ngày");
        btnClearDate.setVisibility(View.GONE);
        applyFilter();
    }

    // ── Dialog xác nhận hành động ─────────────────────────────────────────────

    private void confirmDialog(BookingModel b) {
        String info = formatBookingInfo(b);
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận lịch hẹn")
                .setMessage("Xác nhận lịch hẹn sau?\n\n" + info)
                .setPositiveButton("Xác nhận", (d, w) -> doConfirm(b))
                .setNegativeButton("Đóng", null)
                .show();
    }

    private void rejectDialog(BookingModel b) {
        android.widget.EditText etReason = new android.widget.EditText(this);
        etReason.setHint("Lý do từ chối (tùy chọn)");
        etReason.setPadding(48, 24, 48, 24);
        new AlertDialog.Builder(this)
                .setTitle("Từ chối lịch hẹn")
                .setMessage("Từ chối: " + formatBookingInfo(b))
                .setView(etReason)
                .setPositiveButton("Từ chối", (d, w) -> {
                    String reason = etReason.getText().toString().trim();
                    doReject(b, reason.isEmpty() ? null : reason);
                })
                .setNegativeButton("Đóng", null)
                .show();
    }

    private void startDialog(BookingModel b) {
        new AlertDialog.Builder(this)
                .setTitle("Bắt đầu thực hiện")
                .setMessage("Chuyển lịch hẹn này sang trạng thái Đang thực hiện?\n\n" + formatBookingInfo(b))
                .setPositiveButton("Bắt đầu", (d, w) -> doStart(b))
                .setNegativeButton("Đóng", null)
                .show();
    }

    private void completeDialog(BookingModel b) {
        new AlertDialog.Builder(this)
                .setTitle("Đánh dấu hoàn thành")
                .setMessage("Đánh dấu lịch hẹn này là hoàn thành?\n\n" + formatBookingInfo(b))
                .setPositiveButton("Hoàn thành", (d, w) -> doComplete(b))
                .setNegativeButton("Đóng", null)
                .show();
    }

    private void noShowDialog(BookingModel b) {
        new AlertDialog.Builder(this)
                .setTitle("Khách không đến")
                .setMessage("Đánh dấu khách không đến?\n\n" + formatBookingInfo(b))
                .setPositiveButton("Xác nhận", (d, w) -> doNoShow(b))
                .setNegativeButton("Đóng", null)
                .show();
    }

    // ── Gọi API hành động ─────────────────────────────────────────────────────

    private void doConfirm(BookingModel b) {
        apiService.adminConfirmBooking(b.id).enqueue(new Callback<BookingModel>() {
            @Override public void onResponse(Call<BookingModel> c, Response<BookingModel> resp) {
                if (resp.isSuccessful()) {
                    Toast.makeText(AdminBookingActivity.this, "Đã xác nhận lịch hẹn", Toast.LENGTH_SHORT).show();
                    loadBookings();
                } else {
                    Toast.makeText(AdminBookingActivity.this, parseError(resp), Toast.LENGTH_LONG).show();
                }
            }
            @Override public void onFailure(Call<BookingModel> c, Throwable t) {
                Toast.makeText(AdminBookingActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void doReject(BookingModel b, String reason) {
        apiService.adminRejectBooking(b.id, new CancelRequest(reason)).enqueue(new Callback<BookingModel>() {
            @Override public void onResponse(Call<BookingModel> c, Response<BookingModel> resp) {
                if (resp.isSuccessful()) {
                    Toast.makeText(AdminBookingActivity.this, "Đã từ chối lịch hẹn", Toast.LENGTH_SHORT).show();
                    loadBookings();
                } else {
                    Toast.makeText(AdminBookingActivity.this, parseError(resp), Toast.LENGTH_LONG).show();
                }
            }
            @Override public void onFailure(Call<BookingModel> c, Throwable t) {
                Toast.makeText(AdminBookingActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void doStart(BookingModel b) {
        apiService.adminStartBooking(b.id).enqueue(new Callback<BookingModel>() {
            @Override public void onResponse(Call<BookingModel> c, Response<BookingModel> resp) {
                if (resp.isSuccessful()) {
                    Toast.makeText(AdminBookingActivity.this, "Đã chuyển sang Đang thực hiện", Toast.LENGTH_SHORT).show();
                    loadBookings();
                } else {
                    Toast.makeText(AdminBookingActivity.this, parseError(resp), Toast.LENGTH_LONG).show();
                }
            }
            @Override public void onFailure(Call<BookingModel> c, Throwable t) {
                Toast.makeText(AdminBookingActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void doComplete(BookingModel b) {
        apiService.adminCompleteBooking(b.id).enqueue(new Callback<BookingModel>() {
            @Override public void onResponse(Call<BookingModel> c, Response<BookingModel> resp) {
                if (resp.isSuccessful()) {
                    Toast.makeText(AdminBookingActivity.this, "Đã hoàn thành", Toast.LENGTH_SHORT).show();
                    loadBookings();
                } else {
                    Toast.makeText(AdminBookingActivity.this, parseError(resp), Toast.LENGTH_LONG).show();
                }
            }
            @Override public void onFailure(Call<BookingModel> c, Throwable t) {
                Toast.makeText(AdminBookingActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void doNoShow(BookingModel b) {
        apiService.adminMarkNoShow(b.id).enqueue(new Callback<BookingModel>() {
            @Override public void onResponse(Call<BookingModel> c, Response<BookingModel> resp) {
                if (resp.isSuccessful()) {
                    Toast.makeText(AdminBookingActivity.this, "Đã đánh dấu không đến", Toast.LENGTH_SHORT).show();
                    loadBookings();
                } else {
                    Toast.makeText(AdminBookingActivity.this, parseError(resp), Toast.LENGTH_LONG).show();
                }
            }
            @Override public void onFailure(Call<BookingModel> c, Throwable t) {
                Toast.makeText(AdminBookingActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private String formatBookingInfo(BookingModel b) {
        StringBuilder sb = new StringBuilder();
        if (b.barber != null) sb.append("Thợ: ").append(b.barber.name).append("\n");
        if (b.service != null) sb.append("Dịch vụ: ").append(b.service.name).append("\n");
        if (b.bookingTime != null) sb.append("Giờ hẹn: ").append(formatDateTime(b.bookingTime));
        return sb.toString();
    }

    private String formatDateTime(String iso) {
        try {
            String[] parts = iso.split("T");
            String[] d = parts[0].split("-");
            return parts[1].substring(0, 5) + " — " + d[2] + "/" + d[1] + "/" + d[0];
        } catch (Exception e) { return iso; }
    }

    private String parseError(Response<?> resp) {
        try {
            if (resp.errorBody() != null) {
                JSONObject obj = new JSONObject(resp.errorBody().string());
                if (obj.has("error")) return obj.getString("error");
            }
        } catch (Exception ignored) {}
        return "Thao tác thất bại";
    }

    private void showEmpty(boolean empty) {
        tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        rvBookings.setVisibility(empty ? View.GONE : View.VISIBLE);
    }
}