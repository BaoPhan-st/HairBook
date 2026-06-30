package com.haircut.app.fragment;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import com.haircut.app.R;
import com.haircut.app.adapter.BookingAdapter;
import com.haircut.app.api.ApiClient;
import com.haircut.app.api.ApiService;
import com.haircut.app.model.BookingModel;
import com.haircut.app.model.CancelRequest;
import com.haircut.app.model.RescheduleRequest;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * HistoryFragment — TV5 implement
 * Hiển thị lịch sử đặt lịch, filter theo trạng thái, hỗ trợ hủy + đổi lịch.
 */
public class HistoryFragment extends Fragment {

    private RecyclerView rvBookings;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private TabLayout tabLayout;
    private BookingAdapter adapter;
    private List<BookingModel> allBookings = new ArrayList<>();
    private ApiService apiService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        apiService = ApiClient.getService(requireContext());

        rvBookings  = view.findViewById(R.id.rv_bookings);
        progressBar = view.findViewById(R.id.progress_bar);
        tvEmpty     = view.findViewById(R.id.tv_empty);
        tabLayout   = view.findViewById(R.id.tab_layout);

        rvBookings.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new BookingAdapter(new ArrayList<>(), this::confirmCancel, this::startReschedule);
        rvBookings.setAdapter(adapter);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) { filterBookings(tab.getPosition()); }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        loadBookings();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadBookings(); // Refresh khi quay lại
    }

    private void loadBookings() {
        progressBar.setVisibility(View.VISIBLE);
        apiService.getMyBookings().enqueue(new Callback<List<BookingModel>>() {
            @Override
            public void onResponse(Call<List<BookingModel>> call, Response<List<BookingModel>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    allBookings = response.body();
                    filterBookings(tabLayout.getSelectedTabPosition());
                } else {
                    showEmpty(true);
                }
            }
            @Override
            public void onFailure(Call<List<BookingModel>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Lỗi tải lịch sử", Toast.LENGTH_SHORT).show();
                showEmpty(true);
            }
        });
    }

    // Vị trí tab: 0 Tất cả | 1 Chờ xác nhận | 2 Đã xác nhận | 3 Đang thực hiện | 4 Hoàn thành | 5 Đã huỷ
    private void filterBookings(int tabPosition) {
        List<BookingModel> filtered;
        switch (tabPosition) {
            case 1: filtered = filter("PENDING"); break;
            case 2: filtered = filter("CONFIRMED"); break;
            case 3: filtered = filter("IN_PROGRESS"); break;
            case 4: filtered = filter("COMPLETED"); break;
            case 5: filtered = filter("CANCELLED_BY_CUSTOMER", "CANCELLED_BY_SALON", "NO_SHOW"); break;
            default: filtered = new ArrayList<>(allBookings); break;
        }
        adapter.updateData(filtered);
        showEmpty(filtered.isEmpty());
    }

    private List<BookingModel> filter(String... statuses) {
        List<BookingModel> result = new ArrayList<>();
        for (BookingModel b : allBookings) {
            for (String s : statuses) {
                if (s.equals(b.status)) { result.add(b); break; }
            }
        }
        return result;
    }

    // ───────────────────────────────────────── Hủy lịch ─────────────────────────────────────────

    private void confirmCancel(BookingModel booking) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Hủy lịch hẹn")
                .setMessage("Bạn có chắc muốn hủy lịch hẹn này không? Hành động này không thể hoàn tác.")
                .setPositiveButton("Hủy lịch", (dialog, which) -> doCancelBooking(booking))
                .setNegativeButton("Đóng", null)
                .show();
    }

    private void doCancelBooking(BookingModel booking) {
        apiService.cancelBooking(booking.id, new CancelRequest("Khách hàng tự hủy"))
                .enqueue(new Callback<BookingModel>() {
                    @Override
                    public void onResponse(Call<BookingModel> call, Response<BookingModel> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(getContext(), "Đã huỷ lịch", Toast.LENGTH_SHORT).show();
                            loadBookings();
                        } else {
                            Toast.makeText(getContext(), parseError(response), Toast.LENGTH_LONG).show();
                        }
                    }
                    @Override
                    public void onFailure(Call<BookingModel> call, Throwable t) {
                        Toast.makeText(getContext(), "Lỗi kết nối khi hủy lịch", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ───────────────────────────────────────── Đổi lịch ─────────────────────────────────────────

    private void startReschedule(BookingModel booking) {
        Calendar now = Calendar.getInstance();

        DatePickerDialog datePicker = new DatePickerDialog(
                requireContext(),
                (dateView, year, month, dayOfMonth) -> {
                    TimePickerDialog timePicker = new TimePickerDialog(
                            requireContext(),
                            (timeView, hour, minute) -> {
                                Calendar chosen = Calendar.getInstance();
                                chosen.set(year, month, dayOfMonth, hour, minute, 0);
                                String iso = String.format(Locale.getDefault(),
                                        "%04d-%02d-%02dT%02d:%02d:00",
                                        year, month + 1, dayOfMonth, hour, minute);
                                doReschedule(booking, iso);
                            },
                            now.get(Calendar.HOUR_OF_DAY),
                            now.get(Calendar.MINUTE),
                            true
                    );
                    timePicker.setTitle("Chọn giờ mới");
                    timePicker.show();
                },
                now.get(Calendar.YEAR),
                now.get(Calendar.MONTH),
                now.get(Calendar.DAY_OF_MONTH)
        );
        datePicker.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);

        Calendar maxDate = Calendar.getInstance();
        maxDate.add(Calendar.DAY_OF_YEAR, 30);
        datePicker.getDatePicker().setMaxDate(maxDate.getTimeInMillis());

        datePicker.setTitle("Chọn ngày mới");
        datePicker.show();
    }

    private void doReschedule(BookingModel booking, String newBookingTimeIso) {
        apiService.rescheduleBooking(booking.id, new RescheduleRequest(newBookingTimeIso))
                .enqueue(new Callback<BookingModel>() {
                    @Override
                    public void onResponse(Call<BookingModel> call, Response<BookingModel> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(getContext(), "Đổi lịch thành công, đang chờ xác nhận lại", Toast.LENGTH_LONG).show();
                            loadBookings();
                        } else {
                            Toast.makeText(getContext(), parseError(response), Toast.LENGTH_LONG).show();
                        }
                    }
                    @Override
                    public void onFailure(Call<BookingModel> call, Throwable t) {
                        Toast.makeText(getContext(), "Lỗi kết nối khi đổi lịch", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private String parseError(Response<?> resp) {
        try {
            if (resp.errorBody() != null) {
                String raw = resp.errorBody().string();
                JSONObject obj = new JSONObject(raw);
                if (obj.has("error")) return obj.getString("error");
            }
        } catch (Exception ignored) { }
        return "Thao tác thất bại, vui lòng thử lại";
    }

    private void showEmpty(boolean empty) {
        tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        rvBookings.setVisibility(empty ? View.GONE : View.VISIBLE);
    }
}