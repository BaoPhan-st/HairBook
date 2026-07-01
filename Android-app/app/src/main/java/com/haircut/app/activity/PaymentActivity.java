package com.haircut.app.activity;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.haircut.app.R;
import com.haircut.app.api.ApiClient;
import com.haircut.app.model.PaymentRequest;
import com.haircut.app.model.PaymentResponse;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PaymentActivity extends AppCompatActivity {

    public static final String EXTRA_BOOKING_ID = "booking_id";
    public static final String EXTRA_AMOUNT = "amount";
    public static final String EXTRA_SERVICE = "service_name";
    private static final String RETURN_URL = "hairbook://payment";
    private long bookingId;
    private double amount;
    private TextView tvAmount, tvService, tvStatus;
    private Button btnVnpay, btnMomo;
    private ImageButton btnBack;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        bookingId = getIntent().getLongExtra(EXTRA_BOOKING_ID, -1);
        amount = getIntent().getDoubleExtra(EXTRA_AMOUNT, 0);
        String serviceName = getIntent().getStringExtra(EXTRA_SERVICE);
        if (bookingId == -1) {
            finish();
            return;
        }
        tvAmount = findViewById(R.id.tv_amount);
        tvService = findViewById(R.id.tv_service_name);
        tvStatus = findViewById(R.id.tv_payment_status);
        btnVnpay = findViewById(R.id.btn_vnpay);
        btnMomo = findViewById(R.id.btn_momo);
        btnBack = findViewById(R.id.btn_back);
        progressBar = findViewById(R.id.progress_bar);

        tvAmount.setText(String.format("%.0f đ", amount));
        if (serviceName != null) tvService.setText(serviceName);

        btnVnpay.setOnClickListener(v -> pay("VNPAY"));
        btnMomo.setOnClickListener(v -> pay("MOMO"));
        btnBack.setOnClickListener(v -> finish());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Uri data = intent.getData();
        if (data != null && "hairbook".equals(data.getScheme())) {
            String status = data.getQueryParameter("status");
            String orderId = data.getQueryParameter("orderId");

            if ("success".equalsIgnoreCase(status)) {
                showStatus("✅ Thanh toán thành công!\nMã đơn: " + orderId, true);
            } else {
                showStatus("❌ Thanh toán thất bại. Vui lòng thử lại.", false);
            }
        }
    }

    private void pay(String method) {
        setLoading(true);
        tvStatus.setVisibility(View.GONE);

        PaymentRequest req = new PaymentRequest();
        req.bookingId = bookingId;
        req.method = method;
        req.returnUrl = RETURN_URL;

        ApiClient.getService(this).createPayment(req).enqueue(new Callback<PaymentResponse>() {
            @Override
            public void onResponse(Call<PaymentResponse> call, Response<PaymentResponse> r) {
                setLoading(false);
                if (r.isSuccessful() && r.body() != null && r.body().paymentUrl != null) {
                    try {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                                Uri.parse(r.body().paymentUrl));
                        startActivity(browserIntent);
                    } catch (ActivityNotFoundException e) {
                        String appName = "MOMO".equals(method) ? "MoMo" : "trình duyệt";
                        Toast.makeText(PaymentActivity.this,
                                "Không tìm thấy ứng dụng " + appName + " trên máy. " +
                                        "Vui lòng cài đặt hoặc chọn phương thức khác.",
                                Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(PaymentActivity.this,
                            extractErrorMessage(r), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<PaymentResponse> call, Throwable t) {
                setLoading(false);
                Toast.makeText(PaymentActivity.this,
                        "Lỗi kết nối server", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String extractErrorMessage(Response<PaymentResponse> r) {
        try {
            if (r.errorBody() != null) {
                String json = r.errorBody().string();
                Map<?, ?> map = new Gson().fromJson(json, Map.class);
                Object error = map.get("error");
                if (error != null) return error.toString();
            }
        } catch (Exception ignored) {
        }
        return "Không tạo được link thanh toán";
    }

    private void showStatus(String msg, boolean success) {
        tvStatus.setVisibility(View.VISIBLE);
        tvStatus.setText(msg);
        tvStatus.setTextColor(getResources().getColor(
                success ? android.R.color.holo_green_dark
                        : android.R.color.holo_red_dark, null));
        btnVnpay.setEnabled(!success);
        btnMomo.setEnabled(!success);
    }

    private void setLoading(boolean on) {
        progressBar.setVisibility(on ? View.VISIBLE : View.GONE);
        btnVnpay.setEnabled(!on);
        btnMomo.setEnabled(!on);
    }
}