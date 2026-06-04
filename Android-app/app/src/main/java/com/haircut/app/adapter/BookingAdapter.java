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

    public interface OnCancelClick { void onCancel(Long bookingId); }

    private List<BookingModel> bookings;
    private final OnCancelClick listener;

    public BookingAdapter(List<BookingModel> bookings, OnCancelClick listener) {
        this.bookings = bookings;
        this.listener = listener;
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
        }

        setStatusUI(holder, b.status);

        if ("PENDING".equals(b.status)) {
            holder.btnCancel.setVisibility(View.VISIBLE);
            holder.btnCancel.setOnClickListener(v -> listener.onCancel(b.id));
        } else {
            holder.btnCancel.setVisibility(View.GONE);
        }

        if ("COMPLETED".equals(b.status) || "CANCELLED".equals(b.status)) {
            holder.btnRebook.setVisibility(View.VISIBLE);
        } else {
            holder.btnRebook.setVisibility(View.GONE);
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
        TextView tvBookingTime, tvStatus, tvServiceName, tvPrice, tvBarberName, tvNote;
        Button btnCancel, btnRebook;
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
        }
    }
}
