package com.haircut.app.admin;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.haircut.app.R;
import com.haircut.app.api.ApiClient;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminUsersActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private RecyclerView recyclerView;
    private TextView tvEmpty;
    private EditText etSearch;

    private AdminUserAdapter adapter;
    private final List<AdminUserModel> allUsers = new ArrayList<>();

    // Debounce search để không spam API
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;
    private static final long SEARCH_DELAY_MS = 400;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!ApiClient.isAdmin(this)) {
            finish();
            return;
        }

        setContentView(R.layout.activity_admin_users);
        initViews();
        loadUsers();
    }

    private void initViews() {
        progressBar  = findViewById(R.id.progress_bar);
        recyclerView = findViewById(R.id.recycler_users);
        tvEmpty      = findViewById(R.id.tv_empty);
        etSearch     = findViewById(R.id.et_search);

        // Back button
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        // RecyclerView
        adapter = new AdminUserAdapter(new ArrayList<>(), user -> {
            Intent intent = new Intent(this, AdminUserDetailActivity.class);
            intent.putExtra("userId", user.id);
            startActivity(intent);
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Search với debounce
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);
                String query = s.toString().trim();

                if (query.isEmpty()) {
                    adapter.updateList(allUsers);
                    updateEmptyState(allUsers.isEmpty());
                } else {
                    searchRunnable = () -> performSearch(query);
                    searchHandler.postDelayed(searchRunnable, SEARCH_DELAY_MS);
                }
            }
        });
    }

    private void loadUsers() {
        setLoading(true);
        ApiClient.getService(this).getAllUsers()
            .enqueue(new Callback<List<AdminUserModel>>() {
                @Override
                public void onResponse(Call<List<AdminUserModel>> call, Response<List<AdminUserModel>> response) {
                    setLoading(false);
                    if (response.isSuccessful() && response.body() != null) {
                        allUsers.clear();
                        allUsers.addAll(response.body());
                        adapter.updateList(allUsers);
                        updateEmptyState(allUsers.isEmpty());
                    } else {
                        Toast.makeText(AdminUsersActivity.this,
                            "Không thể tải danh sách người dùng", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<List<AdminUserModel>> call, Throwable t) {
                    setLoading(false);
                    Toast.makeText(AdminUsersActivity.this,
                        "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void performSearch(String query) {
        ApiClient.getService(this).searchUsers(query)
            .enqueue(new Callback<List<AdminUserModel>>() {
                @Override
                public void onResponse(Call<List<AdminUserModel>> call, Response<List<AdminUserModel>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        List<AdminUserModel> results = response.body();
                        adapter.updateList(results);
                        updateEmptyState(results.isEmpty());
                    }
                }

                @Override
                public void onFailure(Call<List<AdminUserModel>> call, Throwable t) {
                    Toast.makeText(AdminUsersActivity.this, "Lỗi tìm kiếm", Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void updateEmptyState(boolean isEmpty) {
        tvEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        etSearch.setText("");
        loadUsers();
    }
}
