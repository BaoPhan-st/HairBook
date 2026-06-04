package com.haircut.app.fragment;

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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * HistoryFragment — TV5 implement
 * Hiển thị lịch sử đặt lịch, filter theo trạng thái.
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
        adapter = new BookingAdapter(new ArrayList<>(), this::onCancelBooking);
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

    private void filterBookings(int tabPosition) {
        List<BookingModel> filtered;
        switch (tabPosition) {
            case 1: filtered = filter("PENDING"); break;
            case 2: filtered = filter("CONFIRMED", "COMPLETED"); break;
            case 3: filtered = filter("CANCELLED"); break;
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

    private void onCancelBooking(Long bookingId) {
        apiService.cancelBooking(bookingId).enqueue(new Callback<BookingModel>() {
            @Override
            public void onResponse(Call<BookingModel> call, Response<BookingModel> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "Đã huỷ lịch", Toast.LENGTH_SHORT).show();
                    loadBookings();
                }
            }
            @Override
            public void onFailure(Call<BookingModel> call, Throwable t) {
                Toast.makeText(getContext(), "Lỗi khi huỷ lịch", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showEmpty(boolean empty) {
        tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        rvBookings.setVisibility(empty ? View.GONE : View.VISIBLE);
    }
}
