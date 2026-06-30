package com.haircut.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.haircut.app.R;
import com.haircut.app.model.BookingModel;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class BookingAdapter extends RecyclerView.Adapter<BookingAdapter.ViewHolder> {

    public interface OnCancelClick { void onCancel(BookingModel booking); }
    public interface OnRescheduleClick { void onReschedule(BookingModel booking); }

    private List<BookingModel> bookings;
    private final OnCancelClick cancelListener;
    private final OnRescheduleClick rescheduleListener;

    public BookingAdapter(List<BookingModel> bookings, OnCancelClick cancelListener,
                          OnRescheduleClick rescheduleListener) {
        this.bookings = bookings;
        this.cancelListener = cancelListener;
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

        holder.tvBookingTime.setText(formatDateTime(b.bookingTime));

        if (b.service != null) {
            holder.tvServiceName.setText(b.service.name);
            if (b.service.price != null) {
                NumberFormat fmt = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
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

        boolean canModify = "PENDING".equals(b.status) || "CONFIRMED".equals(b.status);

        if (canModify) {
            holder.btnCancel.setVisibility(View.VISIBLE);
            holder.btnCancel.setOnClickListener(v -> cancelListener.onCancel(b));
        } else {
            holder.btnCancel.setVisibility(View.GONE);
        }

        if (canModify) {
            holder.btnReschedule.setVisibility(View.VISIBLE);
            holder.btnReschedule.setOnClickListener(v -> rescheduleListener.onReschedule(b));
        } else {
            holder.btnReschedule.setVisibility(View.GONE);
        }

        boolean isFinalState = "COMPLETED".equals(b.status)
                || "CANCELLED_BY_CUSTOMER".equals(b.status)
                || "CANCELLED_BY_SALON".equals(b.status)
                || "NO_SHOW".equals(b.status);
        holder.btnRebook.setVisibility(isFinalState ? View.VISIBLE : View.GONE);
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
            case "IN_PROGRESS":
                holder.tvStatus.setText("Đang thực hiện");
                holder.tvStatus.setTextColor(0xFF2196F3);
                break;
            case "COMPLETED":
                holder.tvStatus.setText("Hoàn thành");
                holder.tvStatus.setTextColor(0xFF2196F3);
                break;
            case "CANCELLED_BY_CUSTOMER":
                holder.tvStatus.setText("Đã huỷ");
                holder.tvStatus.setTextColor(0xFFF44336);
                break;
            case "CANCELLED_BY_SALON":
                holder.tvStatus.setText("Salon đã huỷ");
                holder.tvStatus.setTextColor(0xFFF44336);
                break;
            case "NO_SHOW":
                holder.tvStatus.setText("Không đến");
                holder.tvStatus.setTextColor(0xFF9E9E9E);
                break;
            default:
                holder.tvStatus.setText(status);
                holder.tvStatus.setTextColor(0xFF9E9E9E);
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
        TextView tvBookingTime, tvStatus, tvServiceName, tvPrice, tvBarberName, tvNote;
        Button btnCancel, btnRebook, btnReschedule;
        ViewHolder(View v) {
            super(v);
            tvBookingTime = v.findViewById(R.id.tv_booking_time);
            tvStatus      = v.findViewById(R.id.tv_status);
            tvServiceName = v.findViewById(R.id.tv_service_name);
            tvPrice       = v.findViewById(R.id.tv_price);
            tvBarberName  = v.findViewById(R.id.tv_barber_name);
            tvNote        = v.findViewById(R.id.tv_note);
            btnCancel     = v.findViewById(R.id.btn_cancel);
            btnRebook     = v.findViewById(R.id.btn_rebook);
            btnReschedule = v.findViewById(R.id.btn_reschedule);
        }
    }
}