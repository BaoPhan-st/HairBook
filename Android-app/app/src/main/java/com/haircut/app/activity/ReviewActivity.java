package com.haircut.app.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.haircut.app.R;
import com.haircut.app.api.ApiClient;
import com.haircut.app.model.ReviewModel;
import com.haircut.app.model.ReviewRequest;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReviewActivity extends AppCompatActivity {

    public static final String EXTRA_BOOKING_ID = "booking_id";
    public static final String EXTRA_BARBER_NAME = "barber_name";
    public static final String EXTRA_SERVICE_NAME = "service_name";

    private RatingBar ratingBar;
    private EditText etComment;
    private Button btnSubmit;
    private ProgressBar progressBar;
    private TextView tvTitle;
    private long bookingId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review);
        bookingId = getIntent().getLongExtra(EXTRA_BOOKING_ID, -1);
        if (bookingId == -1) {
            finish();
            return;
        }
        String barberName = getIntent().getStringExtra(EXTRA_BARBER_NAME);
        String serviceName = getIntent().getStringExtra(EXTRA_SERVICE_NAME);
        initViews(barberName, serviceName);
        loadExistingReview();
    }

    private void initViews(String barberName, String serviceName) {
        tvTitle = findViewById(R.id.tv_review_title);
        ratingBar = findViewById(R.id.rating_bar);
        etComment = findViewById(R.id.et_comment);
        btnSubmit = findViewById(R.id.btn_submit_review);
        progressBar = findViewById(R.id.progress_bar);

        if (barberName != null && serviceName != null) {
            tvTitle.setText("Đánh giá dịch vụ\n" + serviceName + " – " + barberName);
        }

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        btnSubmit.setOnClickListener(v -> submitReview());
    }

    private void loadExistingReview() {
        ApiClient.getService(this).getReviewByBooking(bookingId)
                .enqueue(new Callback<ReviewModel>() {
                    @Override
                    public void onResponse(Call<ReviewModel> call, Response<ReviewModel> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            ReviewModel r = response.body();
                            ratingBar.setRating(r.rating);
                            etComment.setText(r.comment);
                            ratingBar.setIsIndicator(true);
                            etComment.setEnabled(false);
                            btnSubmit.setEnabled(false);
                            btnSubmit.setText("Đã đánh giá ✓");
                        }
                    }

                    @Override
                    public void onFailure(Call<ReviewModel> call, Throwable t) {
                    }
                });
    }

    private void submitReview() {
        int rating = (int) ratingBar.getRating();
        String comment = etComment.getText().toString().trim();

        if (rating == 0) {
            Toast.makeText(this, "Vui lòng chọn số sao", Toast.LENGTH_SHORT).show();
            return;
        }

        ReviewRequest req = new ReviewRequest();
        req.bookingId = bookingId;
        req.rating = rating;
        req.comment = comment;
        setLoading(true);
        ApiClient.getService(this).submitReview(req).enqueue(new Callback<ReviewModel>() {
            @Override
            public void onResponse(Call<ReviewModel> call, Response<ReviewModel> response) {
                setLoading(false);
                if (response.isSuccessful()) {
                    Toast.makeText(ReviewActivity.this,
                            "Cảm ơn bạn đã đánh giá! ⭐", Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    Toast.makeText(ReviewActivity.this,
                            "Gửi đánh giá thất bại, thử lại nhé", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ReviewModel> call, Throwable t) {
                setLoading(false);
                Toast.makeText(ReviewActivity.this,
                        "Lỗi kết nối server", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSubmit.setEnabled(!loading);
        btnSubmit.setText(loading ? "Đang gửi..." : "Gửi đánh giá");
    }
}
