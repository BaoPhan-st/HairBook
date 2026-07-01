package com.haircut.app.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.CalendarView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.haircut.app.R;
import com.haircut.app.adapter.BarberBookingAdapter;
import com.haircut.app.api.ApiClient;
import com.haircut.app.model.BarberBookingModel;
import com.haircut.app.model.BarberScheduleModel;
import com.haircut.app.model.BarberScheduleRequest;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BarberScheduleActivity extends AppCompatActivity {

    private Long barberId;
    private String selectedDate;

    private BarberBookingAdapter bookingAdapter;
    private final List<BarberBookingModel> bookings = new ArrayList<>();

    private TextView tvSelectedDate, tvNoBooking, tvScheduleStatus;
    private TextInputEditText etStartTime, etEndTime;
    private View layoutSchedule, layoutBooking;

    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    // Tab index
    private static final int TAB_SCHEDULE = 0;
    private static final int TAB_BOOKING  = 1;
    private int currentTab = TAB_SCHEDULE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barber_schedule);

        barberId = getIntent().getLongExtra("barberId", -1);
        String barberName = getIntent().getStringExtra("barberName");
        setTitle("Lịch: " + (barberName != null ? barberName : ""));

        bindViews();
        setupTabs();
        setupRecycler();
        setupButtons();

        // Load ngày hôm nay
        selectedDate = sdf.format(new Date());
        tvSelectedDate.setText("Ngày: " + selectedDate);
        loadForCurrentTab();

        CalendarView calendar = findViewById(R.id.calendarView);
        calendar.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar c = Calendar.getInstance();
            c.set(year, month, dayOfMonth);
            selectedDate = sdf.format(c.getTime());
            tvSelectedDate.setText("Ngày: " + selectedDate);
            loadForCurrentTab();
        });
    }

    private void bindViews() {
        tvSelectedDate  = findViewById(R.id.tvSelectedDate);
        tvNoBooking     = findViewById(R.id.tvNoBooking);
        tvScheduleStatus = findViewById(R.id.tvScheduleStatus);
        etStartTime     = findViewById(R.id.etStartTime);
        etEndTime       = findViewById(R.id.etEndTime);
        layoutSchedule  = findViewById(R.id.layoutSchedule);
        layoutBooking   = findViewById(R.id.layoutBooking);
    }

    private void setupTabs() {
        TabLayout tabLayout = findViewById(R.id.tabLayout);
        tabLayout.addTab(tabLayout.newTab().setText("Lịch làm việc"));
        tabLayout.addTab(tabLayout.newTab().setText("Lịch hẹn"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
                layoutSchedule.setVisibility(currentTab == TAB_SCHEDULE ? View.VISIBLE : View.GONE);
                layoutBooking.setVisibility(currentTab == TAB_BOOKING ? View.VISIBLE : View.GONE);
                loadForCurrentTab();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupRecycler() {
        RecyclerView recycler = findViewById(R.id.recyclerBookings);
        bookingAdapter = new BarberBookingAdapter(bookings);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(bookingAdapter);
    }

    private void setupButtons() {
        findViewById(R.id.btnSaveSchedule).setOnClickListener(v -> saveSchedule());
        findViewById(R.id.btnDeleteSchedule).setOnClickListener(v -> deleteSchedule());
    }

    private void loadForCurrentTab() {
        if (currentTab == TAB_SCHEDULE) loadSchedule();
        else loadBookings();
    }

    // ── Lịch làm việc ────────────────────────────────────────────────────────

    private void loadSchedule() {
        ApiClient.getService(this)
                .getBarberSchedules(barberId, selectedDate, selectedDate)
                .enqueue(new Callback<List<BarberScheduleModel>>() {
                    @Override
                    public void onResponse(Call<List<BarberScheduleModel>> call,
                                           Response<List<BarberScheduleModel>> response) {
                        if (response.isSuccessful() && response.body() != null
                                && !response.body().isEmpty()) {
                            BarberScheduleModel s = response.body().get(0);
                            etStartTime.setText(s.startTime);
                            etEndTime.setText(s.endTime);
                            tvScheduleStatus.setText("Ca làm: " + s.startTime + " – " + s.endTime);
                            tvScheduleStatus.setTextColor(0xFF388E3C); // xanh lá
                        } else {
                            etStartTime.setText("");
                            etEndTime.setText("");
                            tvScheduleStatus.setText("Chưa có lịch làm ngày này");
                            tvScheduleStatus.setTextColor(0xFFE53935); // đỏ
                        }
                    }
                    @Override
                    public void onFailure(Call<List<BarberScheduleModel>> call, Throwable t) {
                        Toast.makeText(BarberScheduleActivity.this,
                                "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveSchedule() {
        String start = etStartTime.getText() != null ? etStartTime.getText().toString().trim() : "";
        String end   = etEndTime.getText()   != null ? etEndTime.getText().toString().trim()   : "";

        if (start.isEmpty() || end.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập giờ bắt đầu và kết thúc", Toast.LENGTH_SHORT).show();
            return;
        }

        BarberScheduleRequest req = new BarberScheduleRequest(selectedDate, start, end);
        ApiClient.getService(this)
                .setBarberSchedule(barberId, req)
                .enqueue(new Callback<BarberScheduleModel>() {
                    @Override
                    public void onResponse(Call<BarberScheduleModel> call,
                                           Response<BarberScheduleModel> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(BarberScheduleActivity.this,
                                    "Đã lưu lịch làm việc", Toast.LENGTH_SHORT).show();
                            loadSchedule();
                        } else {
                            Toast.makeText(BarberScheduleActivity.this,
                                    "Lưu thất bại: " + response.code(), Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    public void onFailure(Call<BarberScheduleModel> call, Throwable t) {
                        Toast.makeText(BarberScheduleActivity.this,
                                "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void deleteSchedule() {
        ApiClient.getService(this)
                .deleteBarberSchedule(barberId, selectedDate)
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(BarberScheduleActivity.this,
                                    "Đã xóa lịch ngày " + selectedDate, Toast.LENGTH_SHORT).show();
                            etStartTime.setText("");
                            etEndTime.setText("");
                            tvScheduleStatus.setText("Chưa có lịch làm ngày này");
                            tvScheduleStatus.setTextColor(0xFFE53935);
                        } else if (response.code() == 404) {
                            Toast.makeText(BarberScheduleActivity.this,
                                    "Ngày này chưa có lịch để xóa", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(BarberScheduleActivity.this,
                                    "Xóa thất bại: " + response.code(), Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        Toast.makeText(BarberScheduleActivity.this,
                                "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ── Lịch hẹn (booking) ───────────────────────────────────────────────────

    private void loadBookings() {
        ApiClient.getService(this)
                .getBarberBookings(barberId, selectedDate)
                .enqueue(new Callback<List<BarberBookingModel>>() {
                    @Override
                    public void onResponse(Call<List<BarberBookingModel>> call,
                                           Response<List<BarberBookingModel>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            bookings.clear();
                            bookings.addAll(response.body());
                            bookingAdapter.notifyDataSetChanged();
                            tvNoBooking.setVisibility(
                                    bookings.isEmpty() ? View.VISIBLE : View.GONE);
                        }
                    }
                    @Override
                    public void onFailure(Call<List<BarberBookingModel>> call, Throwable t) {
                        Toast.makeText(BarberScheduleActivity.this,
                                "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}