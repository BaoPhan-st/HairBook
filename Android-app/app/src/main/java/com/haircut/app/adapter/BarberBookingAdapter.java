package com.haircut.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.haircut.app.R;
import com.haircut.app.model.BarberBookingModel;

import java.util.List;

public class BarberBookingAdapter extends RecyclerView.Adapter<BarberBookingAdapter.ViewHolder> {

    private final List<BarberBookingModel> items;

    public BarberBookingAdapter(List<BarberBookingModel> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_barber_booking, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        BarberBookingModel b = items.get(position);

        // bookingTime format từ backend: "2025-06-01T09:00:00" → hiển thị giờ
        String time = b.bookingTime != null && b.bookingTime.length() >= 16
                ? b.bookingTime.substring(11, 16)
                : b.bookingTime;
        h.tvTime.setText("🕐 " + time);

        if (b.customer != null) {
            h.tvCustomer.setText("👤 " + b.customer.fullName
                    + (b.customer.phone != null ? " — " + b.customer.phone : ""));
        }

        if (b.service != null) {
            h.tvService.setText("✂️ " + b.service.name
                    + (b.service.price != null ? " — " + b.service.price.longValue() + "đ" : ""));
        }

        h.tvStatus.setText(formatStatus(b.status));
        if (b.note != null && !b.note.isEmpty()) {
            h.tvStatus.append("  |  📝 " + b.note);
        }
    }

    private String formatStatus(String status) {
        if (status == null) return "";
        switch (status) {
            case "PENDING":   return "⏳ Chờ xác nhận";
            case "CONFIRMED": return "✅ Đã xác nhận";
            case "COMPLETED": return "🏁 Hoàn thành";
            case "CANCELLED": return "❌ Đã hủy";
            default:          return status;
        }
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTime, tvCustomer, tvService, tvStatus;
        ViewHolder(View v) {
            super(v);
            tvTime     = v.findViewById(R.id.tvTime);
            tvCustomer = v.findViewById(R.id.tvCustomer);
            tvService  = v.findViewById(R.id.tvService);
            tvStatus   = v.findViewById(R.id.tvBookingStatus);
        }
    }
}