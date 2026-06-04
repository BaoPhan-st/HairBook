package com.haircut.app.activity;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.haircut.app.R;
import com.haircut.app.api.ApiClient;
import com.haircut.app.fragment.HistoryFragment;
import com.haircut.app.fragment.HomeFragment;


public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!ApiClient.isLoggedIn(this)) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);
        setupBottomNavigation();

        // Default: HomeFragment
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
        }
    }

    private void setupBottomNavigation() {
        bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                loadFragment(new HomeFragment());
                return true;
            } else if (id == R.id.nav_book) {
                startActivity(new Intent(this, BookingActivity.class));
                return false; // Không thay đổi tab selection
            } else if (id == R.id.nav_history) {
                loadFragment(new HistoryFragment());
                return true;
            } else if (id == R.id.nav_chat) {
                startActivity(new Intent(this, AIChatActivity.class));
                return false;
            }
            return false;
        });

        // Profile icon trong header
        findViewById(R.id.btn_profile).setOnClickListener(v ->
            startActivity(new Intent(this, ProfileActivity.class)));

        bottomNav.setSelectedItemId(R.id.nav_home);
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit();
    }
}
