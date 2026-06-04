package com.haircut.app.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.haircut.app.R;
import com.haircut.app.adapter.BarberAdapter;
import com.haircut.app.adapter.ServiceAdapter;
import com.haircut.app.adapter.TimeSlotAdapter;
import com.haircut.app.api.ApiClient;
import com.haircut.app.api.ApiService;
import com.haircut.app.model.BarberModel;
import com.haircut.app.model.BookingModel;
import com.haircut.app.model.BookingRequest;
import com.haircut.app.model.ServiceModel;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class BookingActivity extends AppCompatActivity {

    private RecyclerView rvServices, rvBarbers, rvTimeSlots;
    private CalendarView calendarView;
    private Button btnConfirm, btnBack;
    private ProgressBar progressBar;

    private ApiService apiService;
    private Long selectedServiceId;
    private Long selectedBarberId;
    private String selectedDate; // yyyy-MM-dd
    private String selectedSlot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);
        apiService = ApiClient.getService(this);

        initViews();
        loadServices();
        loadBarbers();
        setupCalendar();
    }

    private void initViews() {
        rvServices   = findViewById(R.id.rv_services);
        rvBarbers    = findViewById(R.id.rv_barbers);
        rvTimeSlots  = findViewById(R.id.rv_time_slots);
        calendarView = findViewById(R.id.calendar_view);
        btnConfirm   = findViewById(R.id.btn_confirm_booking);
        progressBar  = findViewById(R.id.progress_bar);

        rvServices.setLayoutManager(new GridLayoutManager(this, 2));
        rvBarbers.setLayoutManager(
            new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvTimeSlots.setLayoutManager(new GridLayoutManager(this, 4));

        // Mặc định hôm nay
        selectedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(new Date());

        btnConfirm.setOnClickListener(v -> attemptBooking());
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }

    private void loadServices() {
        apiService.getAllServices().enqueue(new Callback<List<ServiceModel>>() {
            @Override public void onResponse(Call<List<ServiceModel>> call, Response<List<ServiceModel>> resp) {
                if (resp.isSuccessful() && resp.body() != null) {
                    rvServices.setAdapter(new ServiceAdapter(resp.body(), s -> {
                        selectedServiceId = s.id;
                        Toast.makeText(BookingActivity.this, "Đã chọn: " + s.name, Toast.LENGTH_SHORT).show();
                    }));
                }
            }
            @Override public void onFailure(Call<List<ServiceModel>> call, Throwable t) {
                Toast.makeText(BookingActivity.this, "Lỗi tải dịch vụ", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadBarbers() {
        apiService.getAllBarbers().enqueue(new Callback<List<BarberModel>>() {
            @Override public void onResponse(Call<List<BarberModel>> call, Response<List<BarberModel>> resp) {
                if (resp.isSuccessful() && resp.body() != null) {
                    rvBarbers.setAdapter(new BarberAdapter(resp.body(), b -> {
                        selectedBarberId = b.id;
                        Toast.makeText(BookingActivity.this, "Đã chọn thợ: " + b.name, Toast.LENGTH_SHORT).show();
                        loadTimeSlots();
                    }));
                }
            }
            @Override public void onFailure(Call<List<BarberModel>> call, Throwable t) {}
        });
    }

    private void setupCalendar() {
        calendarView.setMinDate(System.currentTimeMillis() - 1000);
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            selectedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth);
            loadTimeSlots();
        });
    }

    private void loadTimeSlots() {
        if (selectedBarberId == null || selectedDate == null) return;
        apiService.getAvailableSlots(selectedBarberId, selectedDate)
            .enqueue(new Callback<List<String>>() {
                @Override public void onResponse(Call<List<String>> call, Response<List<String>> resp) {
                    if (resp.isSuccessful() && resp.body() != null) {
                        rvTimeSlots.setAdapter(new TimeSlotAdapter(resp.body(), slot -> {
                            selectedSlot = slot;
                        }));
                    }
                }
                @Override public void onFailure(Call<List<String>> call, Throwable t) {}
            });
    }

    private void attemptBooking() {
        if (selectedServiceId == null) { Toast.makeText(this, "Vui lòng chọn dịch vụ", Toast.LENGTH_SHORT).show(); return; }
        if (selectedBarberId == null)  { Toast.makeText(this, "Vui lòng chọn thợ cắt", Toast.LENGTH_SHORT).show(); return; }
        if (selectedSlot == null)      { Toast.makeText(this, "Vui lòng chọn giờ", Toast.LENGTH_SHORT).show(); return; }

        progressBar.setVisibility(View.VISIBLE);
        btnConfirm.setEnabled(false);

        BookingRequest req = new BookingRequest();
        req.serviceId   = selectedServiceId;
        req.barberId    = selectedBarberId;
        req.bookingTime = selectedDate + "T" + selectedSlot + ":00";

        android.widget.EditText etNote = findViewById(R.id.et_note);
        req.note = etNote.getText().toString().trim();

        apiService.createBooking(req).enqueue(new Callback<BookingModel>() {
            @Override public void onResponse(Call<BookingModel> call, Response<BookingModel> resp) {
                progressBar.setVisibility(View.GONE);
                btnConfirm.setEnabled(true);
                if (resp.isSuccessful()) {
                    Toast.makeText(BookingActivity.this, "Đặt lịch thành công! ✓", Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    Toast.makeText(BookingActivity.this, "Đặt lịch thất bại, vui lòng thử lại", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<BookingModel> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                btnConfirm.setEnabled(true);
                Toast.makeText(BookingActivity.this, "Lỗi kết nối server", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
