package com.haircut.app.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.CalendarView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.haircut.app.R;
import com.haircut.app.adapter.BarberBookingAdapter;
import com.haircut.app.api.ApiClient;
import com.haircut.app.model.BarberBookingModel;

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
    private BarberBookingAdapter adapter;
    private final List<BarberBookingModel> bookings = new ArrayList<>();
    private TextView tvSelectedDate, tvNoBooking;
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barber_schedule);

        barberId = getIntent().getLongExtra("barberId", -1);
        String barberName = getIntent().getStringExtra("barberName");
        setTitle("Lịch: " + (barberName != null ? barberName : ""));

        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        tvNoBooking    = findViewById(R.id.tvNoBooking);
        RecyclerView recycler = findViewById(R.id.recyclerBookings);

        adapter = new BarberBookingAdapter(bookings);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        CalendarView calendar = findViewById(R.id.calendarView);

        // Load ngày hôm nay mặc định
        String today = sdf.format(new Date());
        tvSelectedDate.setText("Ngày: " + today);
        loadBookings(today);

        calendar.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar c = Calendar.getInstance();
            c.set(year, month, dayOfMonth);
            String date = sdf.format(c.getTime());
            tvSelectedDate.setText("Ngày: " + date);
            loadBookings(date);
        });
    }

    private void loadBookings(String date) {
        ApiClient.getService(this).getBarberBookings(barberId, date)
                .enqueue(new Callback<List<BarberBookingModel>>() {
                    @Override
                    public void onResponse(Call<List<BarberBookingModel>> call,
                                           Response<List<BarberBookingModel>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            bookings.clear();
                            bookings.addAll(response.body());
                            adapter.notifyDataSetChanged();
                            tvNoBooking.setVisibility(bookings.isEmpty() ? View.VISIBLE : View.GONE);
                        }
                    }

                    @Override
                    public void onFailure(Call<List<BarberBookingModel>> call, Throwable t) {
                        Toast.makeText(BarberScheduleActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}