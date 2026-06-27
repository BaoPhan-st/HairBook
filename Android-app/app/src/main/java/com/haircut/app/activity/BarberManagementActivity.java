package com.haircut.app.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.haircut.app.R;
import com.haircut.app.adapter.BarberAdapter;
import com.haircut.app.api.ApiClient;
import com.haircut.app.model.BarberModel;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BarberManagementActivity extends AppCompatActivity {

    private BarberAdapter adapter;
    private final List<BarberModel> barbers = new ArrayList<>();
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barber_management);

        RecyclerView recycler = findViewById(R.id.recyclerBarbers);
        tvEmpty = findViewById(R.id.tvEmpty);
        FloatingActionButton fab = findViewById(R.id.fabAddBarber);

        adapter = new BarberAdapter(barbers, new BarberAdapter.OnBarberActionListener() {
            @Override
            public void onEdit(BarberModel barber) {
                Intent i = new Intent(BarberManagementActivity.this, BarberFormActivity.class);
                i.putExtra("barberId", barber.id);
                i.putExtra("name", barber.name);
                i.putExtra("specialty", barber.specialty);
                i.putExtra("imageUrl", barber.imageUrl);
                i.putExtra("rating", barber.rating);
                i.putExtra("available", barber.available);
                startActivity(i);
            }

            @Override
            public void onDelete(BarberModel barber) {
                new AlertDialog.Builder(BarberManagementActivity.this)
                        .setTitle("Xác nhận xóa")
                        .setMessage("Xóa thợ " + barber.name + "?")
                        .setPositiveButton("Xóa", (d, w) -> deleteBarber(barber))
                        .setNegativeButton("Hủy", null)
                        .show();
            }

            @Override
            public void onSchedule(BarberModel barber) {
                Intent i = new Intent(BarberManagementActivity.this, BarberScheduleActivity.class);
                i.putExtra("barberId", barber.id);
                i.putExtra("barberName", barber.name);
                startActivity(i);
            }
        });

        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        fab.setOnClickListener(v -> startActivity(
                new Intent(this, BarberFormActivity.class)));

        loadBarbers();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadBarbers();
    }

    private void loadBarbers() {
        ApiClient.getService(this).getAllBarbers().enqueue(new Callback<List<BarberModel>>() {
            @Override
            public void onResponse(Call<List<BarberModel>> call, Response<List<BarberModel>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    barbers.clear();
                    barbers.addAll(response.body());
                    adapter.notifyDataSetChanged();
                    tvEmpty.setVisibility(barbers.isEmpty() ? View.VISIBLE : View.GONE);
                }
            }

            @Override
            public void onFailure(Call<List<BarberModel>> call, Throwable t) {
                Toast.makeText(BarberManagementActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteBarber(BarberModel barber) {
        ApiClient.getService(this).deleteBarber(barber.id).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(BarberManagementActivity.this, "Đã xóa", Toast.LENGTH_SHORT).show();
                    loadBarbers();
                } else {
                    Toast.makeText(BarberManagementActivity.this, "Xóa thất bại", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(BarberManagementActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }
}