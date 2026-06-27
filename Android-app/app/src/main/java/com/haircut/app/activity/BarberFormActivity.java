package com.haircut.app.activity;

import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.haircut.app.R;
import com.haircut.app.api.ApiClient;
import com.haircut.app.model.BarberModel;
import com.haircut.app.model.BarberRequest;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BarberFormActivity extends AppCompatActivity {

    private EditText etName, etSpecialty, etImageUrl, etRating;
    private CheckBox cbAvailable;
    private Long barberId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barber_form);

        etName      = findViewById(R.id.etName);
        etSpecialty = findViewById(R.id.etSpecialty);
        etImageUrl  = findViewById(R.id.etImageUrl);
        etRating    = findViewById(R.id.etRating);
        cbAvailable = findViewById(R.id.cbAvailable);
        Button btnSave = findViewById(R.id.btnSave);

        // Nếu có barberId → chế độ sửa, điền sẵn data
        if (getIntent().hasExtra("barberId")) {
            barberId = getIntent().getLongExtra("barberId", -1);
            etName.setText(getIntent().getStringExtra("name"));
            etSpecialty.setText(getIntent().getStringExtra("specialty"));
            etImageUrl.setText(getIntent().getStringExtra("imageUrl"));
            double rating = getIntent().getDoubleExtra("rating", 0.0);
            etRating.setText(rating > 0 ? String.valueOf(rating) : "");
            cbAvailable.setChecked(getIntent().getBooleanExtra("available", true));
            setTitle("Sửa thông tin thợ");
        } else {
            setTitle("Thêm thợ mới");
        }

        btnSave.setOnClickListener(v -> save());
    }

    private void save() {
        String name = etName.getText().toString().trim();
        if (name.isEmpty()) {
            etName.setError("Bắt buộc");
            return;
        }

        String specialty = etSpecialty.getText().toString().trim();
        String imageUrl  = etImageUrl.getText().toString().trim();
        Double rating    = null;
        try {
            String r = etRating.getText().toString().trim();
            if (!r.isEmpty()) rating = Double.parseDouble(r);
        } catch (NumberFormatException ignored) {}

        BarberRequest req = new BarberRequest(
                name,
                specialty.isEmpty() ? null : specialty,
                imageUrl.isEmpty()  ? null : imageUrl,
                rating,
                cbAvailable.isChecked()
        );

        if (barberId != null) {
            ApiClient.getService(this).updateBarber(barberId, req)
                    .enqueue(new Callback<BarberModel>() {
                        @Override
                        public void onResponse(Call<BarberModel> call, Response<BarberModel> response) {
                            handleResult(response.isSuccessful(), "Cập nhật thành công", "Cập nhật thất bại");
                        }
                        @Override
                        public void onFailure(Call<BarberModel> call, Throwable t) {
                            Toast.makeText(BarberFormActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            ApiClient.getService(this).createBarber(req)
                    .enqueue(new Callback<BarberModel>() {
                        @Override
                        public void onResponse(Call<BarberModel> call, Response<BarberModel> response) {
                            handleResult(response.isSuccessful(), "Thêm thành công", "Thêm thất bại");
                        }
                        @Override
                        public void onFailure(Call<BarberModel> call, Throwable t) {
                            Toast.makeText(BarberFormActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void handleResult(boolean success, String msgOk, String msgFail) {
        Toast.makeText(this, success ? msgOk : msgFail, Toast.LENGTH_SHORT).show();
        if (success) finish();
    }
}