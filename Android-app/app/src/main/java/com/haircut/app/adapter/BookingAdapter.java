package com.haircut.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.haircut.app.R;
import com.haircut.app.model.BookingModel;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class BookingAdapter extends RecyclerView.Adapter<BookingAdapter.ViewHolder> {

    public interface OnCancelClick { void onCancel(Long bookingId); }
    public interface OnReviewClick { void onReview(BookingModel booking); }
    public interface OnPaymentClick { void onPayment(BookingModel booking); }

    private List<BookingModel> bookings;
    private final OnCancelClick cancelListener;
    private final OnReviewClick reviewListener;
    private final OnPaymentClick paymentListener;

    public BookingAdapter(List<BookingModel> bookings, OnCancelClick cancelListener,
                          OnReviewClick reviewListener, OnPaymentClick paymentListener) {
        this.bookings = bookings;
        this.cancelListener = cancelListener;
        this.reviewListener = reviewListener;
        this.paymentListener = paymentListener;
    }

    public void updateData(List<BookingModel> newData) {
        this.bookings = newData;
        notifyDataSetChanged();
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_booking, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BookingModel b = bookings.get(position);

        holder.tvBookingTime.setText(formatDateTime(b.bookingTime));

        if (b.service != null) {
            holder.tvServiceName.setText(b.service.name);
            if (b.service.price != null) {
                NumberFormat fmt = NumberFormat.getNumberInstance(new Locale("vi","VN"));
                holder.tvPrice.setText(fmt.format(b.service.price.longValue()) + "đ");
            }
        }

        if (b.barber != null) holder.tvBarberName.setText(b.barber.name);

        if (b.note != null && !b.note.isEmpty()) {
            holder.tvNote.setText("Ghi chú: " + b.note);
            holder.tvNote.setVisibility(View.VISIBLE);
        } else {
            holder.tvNote.setVisibility(View.GONE);
        }

        setStatusUI(holder, b.status);

        if ("PENDING".equals(b.status)) {
            holder.btnCancel.setVisibility(View.VISIBLE);
            holder.btnCancel.setOnClickListener(v -> cancelListener.onCancel(b.id));
        } else {
            holder.btnCancel.setVisibility(View.GONE);
        }

        if ("COMPLETED".equals(b.status) || "CANCELLED".equals(b.status)) {
            holder.btnRebook.setVisibility(View.VISIBLE);
        } else {
            holder.btnRebook.setVisibility(View.GONE);
        }

        // Thanh toán: hiện khi đơn còn hiệu lực và chưa hoàn thành/huỷ
        if ("PENDING".equals(b.status) || "CONFIRMED".equals(b.status)) {
            holder.btnPayment.setVisibility(View.VISIBLE);
            holder.btnPayment.setOnClickListener(v -> paymentListener.onPayment(b));
        } else {
            holder.btnPayment.setVisibility(View.GONE);
        }

        // Đánh giá: chỉ hiện khi đơn đã hoàn thành
        if ("COMPLETED".equals(b.status)) {
            holder.btnReview.setVisibility(View.VISIBLE);
            holder.btnReview.setText("Đánh giá");
            holder.btnReview.setOnClickListener(v -> reviewListener.onReview(b));
        } else {
            holder.btnReview.setVisibility(View.GONE);
        }

        // Hiện lại đánh giá đã gửi (nếu có) ngay dưới ghi chú
        if (b.review != null && b.review.rating > 0) {
            holder.layoutReview.setVisibility(View.VISIBLE);
            holder.ratingBarReview.setRating(b.review.rating);
            if (b.review.comment != null && !b.review.comment.trim().isEmpty()) {
                holder.tvReviewComment.setText(b.review.comment);
                holder.tvReviewComment.setVisibility(View.VISIBLE);
            } else {
                holder.tvReviewComment.setVisibility(View.GONE);
            }
            if ("COMPLETED".equals(b.status)) {
                holder.btnReview.setText("Sửa đánh giá");
            }
        } else {
            holder.layoutReview.setVisibility(View.GONE);
        }
    }

    private void setStatusUI(ViewHolder holder, String status) {
        if (status == null) return;
        switch (status) {
            case "PENDING":
                holder.tvStatus.setText("Chờ xác nhận");
                holder.tvStatus.setTextColor(0xFFFF9800);
                break;
            case "CONFIRMED":
                holder.tvStatus.setText("Đã xác nhận");
                holder.tvStatus.setTextColor(0xFF4CAF50);
                break;
            case "COMPLETED":
                holder.tvStatus.setText("Hoàn thành");
                holder.tvStatus.setTextColor(0xFF2196F3);
                break;
            case "CANCELLED":
                holder.tvStatus.setText("Đã huỷ");
                holder.tvStatus.setTextColor(0xFFF44336);
                break;
        }
    }

    private String formatDateTime(String iso) {
        if (iso == null) return "";
        try {
            String[] parts = iso.split("T");
            String datePart = parts[0]; // 2024-01-15
            String timePart = parts[1].substring(0, 5); // 10:00
            String[] dateNums = datePart.split("-");
            return timePart + " - " + dateNums[2] + "/" + dateNums[1] + "/" + dateNums[0];
        } catch (Exception e) {
            return iso;
        }
    }

    @Override public int getItemCount() { return bookings.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvBookingTime, tvStatus, tvServiceName, tvPrice, tvBarberName, tvNote, tvReviewComment;
        Button btnCancel, btnRebook, btnPayment, btnReview;
        View layoutReview;
        RatingBar ratingBarReview;
        ViewHolder(View v) {
            super(v);
            tvBookingTime    = v.findViewById(R.id.tv_booking_time);
            tvStatus         = v.findViewById(R.id.tv_status);
            tvServiceName    = v.findViewById(R.id.tv_service_name);
            tvPrice          = v.findViewById(R.id.tv_price);
            tvBarberName     = v.findViewById(R.id.tv_barber_name);
            tvNote           = v.findViewById(R.id.tv_note);
            btnCancel        = v.findViewById(R.id.btn_cancel);
            btnRebook        = v.findViewById(R.id.btn_rebook);
            btnPayment       = v.findViewById(R.id.btn_payment);
            btnReview        = v.findViewById(R.id.btn_review);
            layoutReview     = v.findViewById(R.id.layout_review);
            ratingBarReview  = v.findViewById(R.id.rating_bar_review);
            tvReviewComment  = v.findViewById(R.id.tv_review_comment);
        }
    }
}