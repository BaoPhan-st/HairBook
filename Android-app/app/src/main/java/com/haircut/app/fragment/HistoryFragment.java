package com.haircut.app.fragment;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
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
import com.haircut.app.activity.BookingActivity;
import com.haircut.app.activity.PaymentActivity;
import com.haircut.app.activity.ReviewActivity;
import com.haircut.app.adapter.BookingAdapter;
import com.haircut.app.api.ApiClient;
import com.haircut.app.api.ApiService;
import com.haircut.app.model.BookingModel;
import com.haircut.app.model.CancelRequest;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


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
        adapter = new BookingAdapter(new ArrayList<>(),
                this::onCancelBooking,
                this::onReviewBooking,
                this::onPaymentBooking,
                this::onRebookBooking,
                this::onRescheduleBooking);
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
        loadBookings(); // Refresh khi quay lại (sau khi huỷ/đổi lịch/đặt lại/đánh giá/thanh toán)
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
                    allBookings = new ArrayList<>();
                    adapter.updateData(allBookings);
                    showEmpty(true);
                    Toast.makeText(getContext(), "Không tải được lịch sử, vui lòng thử lại", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<List<BookingModel>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Lỗi kết nối khi tải lịch sử", Toast.LENGTH_SHORT).show();
                showEmpty(true);
            }
        });
    }

    private void filterBookings(int tabPosition) {
        List<BookingModel> filtered;
        switch (tabPosition) {
            case 1: filtered = filter("PENDING"); break;
            case 2: filtered = filter("CONFIRMED"); break;
            case 3: filtered = filter("IN_PROGRESS"); break;
            case 4: filtered = filter("COMPLETED"); break;
            case 5: filtered = filter("CANCELLED_BY_CUSTOMER", "CANCELLED_BY_SALON", "NO_SHOW"); break;
            default: filtered = new ArrayList<>(allBookings); break; // Tất cả
        }
        adapter.updateData(filtered);
        showEmpty(filtered.isEmpty());
    }

    private List<BookingModel> filter(String... statuses) {
        List<BookingModel> result = new ArrayList<>();
        for (BookingModel b : allBookings) {
            if (b.status == null) continue;
            for (String s : statuses) {
                if (s.equals(b.status)) { result.add(b); break; }
            }
        }
        return result;
    }

    // ── Huỷ lịch ─────────────────────────────────────────────────────────────

    private void onCancelBooking(BookingModel booking) {
        if (booking == null || booking.id == null) return;

        EditText etReason = new EditText(getContext());
        etReason.setHint("Lý do huỷ (tuỳ chọn)");
        etReason.setPadding(48, 24, 48, 24);

        new AlertDialog.Builder(getContext())
                .setTitle("Huỷ lịch hẹn")
                .setMessage("Bạn có chắc muốn huỷ lịch hẹn này?")
                .setView(etReason)
                .setPositiveButton("Huỷ lịch", (d, w) -> {
                    String reason = etReason.getText().toString().trim();
                    doCancel(booking.id, reason.isEmpty() ? null : reason);
                })
                .setNegativeButton("Đóng", null)
                .show();
    }

    private void doCancel(Long bookingId, String reason) {
        progressBar.setVisibility(View.VISIBLE);
        apiService.cancelBooking(bookingId, new CancelRequest(reason)).enqueue(new Callback<BookingModel>() {
            @Override
            public void onResponse(Call<BookingModel> call, Response<BookingModel> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "Đã huỷ lịch hẹn", Toast.LENGTH_SHORT).show();
                    loadBookings();
                } else {
                    Toast.makeText(getContext(), parseError(response), Toast.LENGTH_LONG).show();
                }
            }
            @Override
            public void onFailure(Call<BookingModel> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Lỗi kết nối khi huỷ lịch", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ── Đặt lại (Rebook) ─────────────────────────────────────────────────────

    private void onRebookBooking(BookingModel booking) {
        if (booking == null) return;
        Intent intent = new Intent(getContext(), BookingActivity.class);
        intent.putExtra(BookingActivity.EXTRA_MODE, BookingActivity.MODE_REBOOK);
        if (booking.service != null && booking.service.id != null) {
            intent.putExtra(BookingActivity.EXTRA_SERVICE_ID, booking.service.id);
            intent.putExtra(BookingActivity.EXTRA_SERVICE_NAME, booking.service.name);
        }
        if (booking.barber != null && booking.barber.id != null) {
            intent.putExtra(BookingActivity.EXTRA_BARBER_ID, booking.barber.id);
            intent.putExtra(BookingActivity.EXTRA_BARBER_NAME, booking.barber.name);
        }
        if (booking.note != null) {
            intent.putExtra(BookingActivity.EXTRA_NOTE, booking.note);
        }
        startActivity(intent);
    }

    // ── Đổi lịch (Reschedule) ────────────────────────────────────────────────

    private void onRescheduleBooking(BookingModel booking) {
        if (booking == null || booking.id == null) return;
        Intent intent = new Intent(getContext(), BookingActivity.class);
        intent.putExtra(BookingActivity.EXTRA_MODE, BookingActivity.MODE_RESCHEDULE);
        intent.putExtra(BookingActivity.EXTRA_BOOKING_ID, booking.id);
        if (booking.service != null && booking.service.id != null) {
            intent.putExtra(BookingActivity.EXTRA_SERVICE_ID, booking.service.id);
            intent.putExtra(BookingActivity.EXTRA_SERVICE_NAME, booking.service.name);
        }
        if (booking.barber != null && booking.barber.id != null) {
            intent.putExtra(BookingActivity.EXTRA_BARBER_ID, booking.barber.id);
            intent.putExtra(BookingActivity.EXTRA_BARBER_NAME, booking.barber.name);
        }
        startActivity(intent);
    }

    // ── Đánh giá / Thanh toán ────────────────────────────────────────────────

    private void onReviewBooking(BookingModel booking) {
        Intent intent = new Intent(getContext(), ReviewActivity.class);
        intent.putExtra(ReviewActivity.EXTRA_BOOKING_ID, booking.id);
        if (booking.barber != null) {
            intent.putExtra(ReviewActivity.EXTRA_BARBER_NAME, booking.barber.name);
        }
        if (booking.service != null) {
            intent.putExtra(ReviewActivity.EXTRA_SERVICE_NAME, booking.service.name);
        }
        startActivity(intent);
    }

    private void onPaymentBooking(BookingModel booking) {
        Intent intent = new Intent(getContext(), PaymentActivity.class);
        intent.putExtra(PaymentActivity.EXTRA_BOOKING_ID, booking.id);
        if (booking.service != null) {
            if (booking.service.price != null) {
                intent.putExtra(PaymentActivity.EXTRA_AMOUNT, booking.service.price);
            }
            intent.putExtra(PaymentActivity.EXTRA_SERVICE, booking.service.name);
        }
        startActivity(intent);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private String parseError(Response<?> resp) {
        try {
            if (resp.errorBody() != null) {
                org.json.JSONObject obj = new org.json.JSONObject(resp.errorBody().string());
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