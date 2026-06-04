package com.haircut.app.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.haircut.app.R;
import com.haircut.app.activity.BookingActivity;
import com.haircut.app.adapter.BarberAdapter;
import com.haircut.app.adapter.ServiceAdapter;
import com.haircut.app.api.ApiClient;
import com.haircut.app.api.ApiService;
import com.haircut.app.model.BarberModel;
import com.haircut.app.model.ServiceModel;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * HomeFragment — TV4 implement danh sách dịch vụ & thợ
 * Nhóm trưởng tạo khung + kết nối API.
 */
public class HomeFragment extends Fragment {

    private RecyclerView rvServices, rvBarbers;
    private ProgressBar progressBar;
    private ApiService apiService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        apiService = ApiClient.getService(requireContext());

        rvServices  = view.findViewById(R.id.rv_services);
        rvBarbers   = view.findViewById(R.id.rv_barbers);
        progressBar = view.findViewById(R.id.progress_bar);

        rvServices.setLayoutManager(
            new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvBarbers.setLayoutManager(
            new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

        view.findViewById(R.id.btn_book_now).setOnClickListener(v ->
            startActivity(new Intent(getActivity(), BookingActivity.class)));

        loadData();
    }

    private void loadData() {
        progressBar.setVisibility(View.VISIBLE);
        loadServices();
        loadBarbers();
    }

    private void loadServices() {
        apiService.getAllServices().enqueue(new Callback<List<ServiceModel>>() {
            @Override
            public void onResponse(Call<List<ServiceModel>> call, Response<List<ServiceModel>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    rvServices.setAdapter(new ServiceAdapter(response.body(), service ->
                        startActivity(new Intent(getActivity(), BookingActivity.class))));
                }
            }
            @Override
            public void onFailure(Call<List<ServiceModel>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Lỗi tải dịch vụ", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadBarbers() {
        apiService.getAllBarbers().enqueue(new Callback<List<BarberModel>>() {
            @Override
            public void onResponse(Call<List<BarberModel>> call, Response<List<BarberModel>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    rvBarbers.setAdapter(new BarberAdapter(response.body(), barber -> {}));
                }
            }
            @Override
            public void onFailure(Call<List<BarberModel>> call, Throwable t) {}
        });
    }
}
