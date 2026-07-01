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

    public interface OnCancelClick      { void onCancel(BookingModel booking); }
    public interface OnReviewClick      { void onReview(BookingModel booking); }
    public interface OnPaymentClick     { void onPayment(BookingModel booking); }
    public interface OnRebookClick      { void onRebook(BookingModel booking); }
    public interface OnRescheduleClick  { void onReschedule(BookingModel booking); }

    private List<BookingModel> bookings;
    private final OnCancelClick     cancelListener;
    private final OnReviewClick     reviewListener;
    private final OnPaymentClick    paymentListener;
    private final OnRebookClick     rebookListener;
    private final OnRescheduleClick rescheduleListener;

    public BookingAdapter(List<BookingModel> bookings,
                          OnCancelClick cancelListener,
                          OnReviewClick reviewListener,
                          OnPaymentClick paymentListener,
                          OnRebookClick rebookListener,
                          OnRescheduleClick rescheduleListener) {
        this.bookings = bookings;
        this.cancelListener = cancelListener;
        this.reviewListener = reviewListener;
        this.paymentListener = paymentListener;
        this.rebookListener = rebookListener;
        this.rescheduleListener = rescheduleListener;
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
        String status = b.status != null ? b.status : "";

        holder.tvBookingTime.setText(formatDateTime(b.bookingTime));

        if (b.service != null) {
            holder.tvServiceName.setText(b.service.name != null ? b.service.name : "");
            if (b.service.price != null) {
                NumberFormat fmt = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
                holder.tvPrice.setText(fmt.format(b.service.price.longValue()) + "đ");
            } else {
                holder.tvPrice.setText("");
            }
        } else {
            holder.tvServiceName.setText("");
            holder.tvPrice.setText("");
        }

        holder.tvBarberName.setText(b.barber != null && b.barber.name != null ? b.barber.name : "—");

        if (b.note != null && !b.note.trim().isEmpty()) {
            holder.tvNote.setText("Ghi chú: " + b.note);
            holder.tvNote.setVisibility(View.VISIBLE);
        } else {
            holder.tvNote.setVisibility(View.GONE);
        }

        setStatusUI(holder, status);

        // ── Reset toàn bộ nút trước khi áp quy tắc mới (an toàn khi ViewHolder tái sử dụng) ──
        holder.btnCancel.setVisibility(View.GONE);
        holder.btnCancel.setOnClickListener(null);
        holder.btnReschedule.setVisibility(View.GONE);
        holder.btnReschedule.setOnClickListener(null);
        holder.btnRebook.setVisibility(View.GONE);
        holder.btnRebook.setOnClickListener(null);
        holder.btnPayment.setVisibility(View.GONE);
        holder.btnPayment.setOnClickListener(null);
        holder.btnReview.setVisibility(View.GONE);
        holder.btnReview.setOnClickListener(null);

        boolean isPendingOrConfirmed = "PENDING".equals(status) || "CONFIRMED".equals(status);
        boolean isRebookable = "COMPLETED".equals(status)
                || "CANCELLED_BY_CUSTOMER".equals(status)
                || "CANCELLED_BY_SALON".equals(status)
                || "NO_SHOW".equals(status);

        // Huỷ: PENDING hoặc CONFIRMED (server sẽ tự chặn nếu còn dưới 2h trước giờ hẹn)
        if (isPendingOrConfirmed) {
            holder.btnCancel.setVisibility(View.VISIBLE);
            holder.btnCancel.setOnClickListener(v -> cancelListener.onCancel(b));
        }

        // Đổi lịch: PENDING hoặc CONFIRMED
        if (isPendingOrConfirmed) {
            holder.btnReschedule.setVisibility(View.VISIBLE);
            holder.btnReschedule.setOnClickListener(v -> rescheduleListener.onReschedule(b));
        }

        // Đặt lại: COMPLETED, CANCELLED_BY_CUSTOMER, CANCELLED_BY_SALON, NO_SHOW
        if (isRebookable) {
            holder.btnRebook.setVisibility(View.VISIBLE);
            holder.btnRebook.setOnClickListener(v -> rebookListener.onRebook(b));
        }

        // Thanh toán: PENDING hoặc CONFIRMED
        if (isPendingOrConfirmed) {
            holder.btnPayment.setVisibility(View.VISIBLE);
            holder.btnPayment.setOnClickListener(v -> paymentListener.onPayment(b));
        }

        // Đánh giá: chỉ khi đã COMPLETED
        if ("COMPLETED".equals(status)) {
            holder.btnReview.setVisibility(View.VISIBLE);
            holder.btnReview.setText(b.review != null && b.review.rating > 0 ? "Sửa đánh giá" : "Đánh giá");
            holder.btnReview.setOnClickListener(v -> reviewListener.onReview(b));
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
        } else {
            holder.layoutReview.setVisibility(View.GONE);
        }
    }

    private void setStatusUI(ViewHolder holder, String status) {
        switch (status) {
            case "PENDING":
                holder.tvStatus.setText("Chờ xác nhận");
                holder.tvStatus.setTextColor(0xFFFF9800);
                break;
            case "CONFIRMED":
                holder.tvStatus.setText("Đã xác nhận");
                holder.tvStatus.setTextColor(0xFF4CAF50);
                break;
            case "IN_PROGRESS":
                holder.tvStatus.setText("Đang thực hiện");
                holder.tvStatus.setTextColor(0xFF2196F3);
                break;
            case "COMPLETED":
                holder.tvStatus.setText("Hoàn thành");
                holder.tvStatus.setTextColor(0xFF2196F3);
                break;
            case "CANCELLED_BY_CUSTOMER":
                holder.tvStatus.setText("Bạn đã hủy");
                holder.tvStatus.setTextColor(0xFFF44336);
                break;
            case "CANCELLED_BY_SALON":
                holder.tvStatus.setText("Salon đã hủy");
                holder.tvStatus.setTextColor(0xFFF44336);
                break;
            case "NO_SHOW":
                holder.tvStatus.setText("Không đến");
                holder.tvStatus.setTextColor(0xFF9E9E9E);
                break;
            default:
                holder.tvStatus.setText(status);
                holder.tvStatus.setTextColor(0xFF9E9E9E);
        }
    }

    private String formatDateTime(String iso) {
        if (iso == null) return "";
        try {
            String[] parts = iso.split("T");
            String datePart = parts[0]; // 2026-01-15
            String timePart = parts[1].substring(0, 5); // 10:00
            String[] dateNums = datePart.split("-");
            return timePart + " - " + dateNums[2] + "/" + dateNums[1] + "/" + dateNums[0];
        } catch (Exception e) {
            return iso;
        }
    }

    @Override public int getItemCount() { return bookings == null ? 0 : bookings.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvBookingTime, tvStatus, tvServiceName, tvPrice, tvBarberName, tvNote, tvReviewComment;
        Button btnCancel, btnReschedule, btnRebook, btnPayment, btnReview;
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
            btnReschedule    = v.findViewById(R.id.btn_reschedule);
            btnRebook        = v.findViewById(R.id.btn_rebook);
            btnPayment       = v.findViewById(R.id.btn_payment);
            btnReview        = v.findViewById(R.id.btn_review);
            layoutReview     = v.findViewById(R.id.layout_review);
            ratingBarReview  = v.findViewById(R.id.rating_bar_review);
            tvReviewComment  = v.findViewById(R.id.tv_review_comment);
        }
    }
}