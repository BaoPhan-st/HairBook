package com.haircut.app.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.haircut.app.R;
import com.haircut.app.model.BookingModel;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class AdminBookingAdapter extends RecyclerView.Adapter<AdminBookingAdapter.ViewHolder> {

    public interface ActionListener {
        void onConfirm(BookingModel booking);
        void onReject(BookingModel booking);
        void onStart(BookingModel booking);
        void onComplete(BookingModel booking);
        void onNoShow(BookingModel booking);
    }

    private List<BookingModel> bookings;
    private final ActionListener listener;

    public AdminBookingAdapter(List<BookingModel> bookings, ActionListener listener) {
        this.bookings = bookings;
        this.listener = listener;
    }

    public void updateData(List<BookingModel> newData) {
        this.bookings = newData;
        notifyDataSetChanged();
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_booking, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        BookingModel b = bookings.get(position);
        String status = b.status != null ? b.status : "";

        // Tên khách + SĐT
        if (b.customer != null) {
            String phone = (b.customer.phone != null && !b.customer.phone.isEmpty())
                    ? " · " + b.customer.phone : "";
            String name = b.customer.fullName != null ? b.customer.fullName : "";
            h.tvCustomerName.setText(name + phone);
        } else {
            h.tvCustomerName.setText("Booking #" + b.id);
        }

        // Thời gian
        h.tvBookingTime.setText("🕐 " + formatDateTime(b.bookingTime));

        // Thợ
        h.tvBarberName.setText(b.barber != null && b.barber.name != null ? "✂ " + b.barber.name : "—");

        // Dịch vụ
        if (b.service != null) {
            NumberFormat fmt = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
            String price = b.service.price != null
                    ? fmt.format(b.service.price.longValue()) + "đ" : "";
            String duration = b.service.durationMinutes != null
                    ? b.service.durationMinutes + " phút" : "";
            h.tvServiceInfo.setText((b.service.name != null ? b.service.name : "") + " — " + price + " — " + duration);
        } else {
            h.tvServiceInfo.setText("");
        }

        // Ghi chú
        if (b.note != null && !b.note.trim().isEmpty()) {
            h.tvNote.setText("📝 " + b.note);
            h.tvNote.setVisibility(View.VISIBLE);
        } else {
            h.tvNote.setVisibility(View.GONE);
        }

        // Lý do hủy
        if (b.cancelReason != null && !b.cancelReason.isEmpty()
                && ("CANCELLED_BY_SALON".equals(status) || "CANCELLED_BY_CUSTOMER".equals(status))) {
            h.tvCancelReason.setText("Lý do: " + b.cancelReason);
            h.tvCancelReason.setVisibility(View.VISIBLE);
        } else {
            h.tvCancelReason.setVisibility(View.GONE);
        }

        // Status badge
        setStatusBadge(h, status);

        // ── Reset toàn bộ action row + listener trước khi áp theo status mới ──
        h.layoutActions.setVisibility(View.GONE);
        h.btnConfirm.setOnClickListener(null);
        h.btnReject.setOnClickListener(null);

        h.layoutActionsConfirmed.setVisibility(View.GONE);
        h.btnStart.setOnClickListener(null);
        h.btnComplete.setOnClickListener(null);
        h.btnNoShow.setOnClickListener(null);

        h.layoutActionsInProgress.setVisibility(View.GONE);
        h.btnCompleteInProgress.setOnClickListener(null);

        if ("PENDING".equals(status)) {
            h.layoutActions.setVisibility(View.VISIBLE);
            h.btnConfirm.setOnClickListener(v -> listener.onConfirm(b));
            h.btnReject.setOnClickListener(v -> listener.onReject(b));

        } else if ("CONFIRMED".equals(status)) {
            // Từ CONFIRMED: có thể Bắt đầu thực hiện, Hoàn thành trực tiếp, hoặc đánh dấu Không đến
            h.layoutActionsConfirmed.setVisibility(View.VISIBLE);
            h.btnStart.setOnClickListener(v -> listener.onStart(b));
            h.btnComplete.setOnClickListener(v -> listener.onComplete(b));
            h.btnNoShow.setOnClickListener(v -> listener.onNoShow(b));

        } else if ("IN_PROGRESS".equals(status)) {
            // Từ IN_PROGRESS: chỉ có thể Hoàn thành (không No-show ở bước này nữa)
            h.layoutActionsInProgress.setVisibility(View.VISIBLE);
            h.btnCompleteInProgress.setOnClickListener(v -> listener.onComplete(b));
        }
        // COMPLETED / CANCELLED_BY_CUSTOMER / CANCELLED_BY_SALON / NO_SHOW: không còn hành động nào (trạng thái cuối)
    }

    private void setStatusBadge(ViewHolder h, String status) {
        switch (status) {
            case "PENDING":
                h.tvStatus.setText("Chờ xác nhận");
                h.tvStatus.setBackgroundColor(0xFFFF9800);
                break;
            case "CONFIRMED":
                h.tvStatus.setText("Đã xác nhận");
                h.tvStatus.setBackgroundColor(0xFF4CAF50);
                break;
            case "IN_PROGRESS":
                h.tvStatus.setText("Đang thực hiện");
                h.tvStatus.setBackgroundColor(0xFF2196F3);
                break;
            case "COMPLETED":
                h.tvStatus.setText("Hoàn thành");
                h.tvStatus.setBackgroundColor(0xFF2196F3);
                break;
            case "CANCELLED_BY_CUSTOMER":
                h.tvStatus.setText("KH đã hủy");
                h.tvStatus.setBackgroundColor(0xFFF44336);
                break;
            case "CANCELLED_BY_SALON":
                h.tvStatus.setText("Salon đã hủy");
                h.tvStatus.setBackgroundColor(0xFFF44336);
                break;
            case "NO_SHOW":
                h.tvStatus.setText("Không đến");
                h.tvStatus.setBackgroundColor(0xFF9E9E9E);
                break;
            default:
                h.tvStatus.setText(status);
                h.tvStatus.setBackgroundColor(0xFF9E9E9E);
        }
    }

    private String formatDateTime(String iso) {
        if (iso == null) return "";
        try {
            String[] parts = iso.split("T");
            String datePart = parts[0];
            String timePart = parts[1].substring(0, 5);
            String[] d = datePart.split("-");
            return timePart + " — " + d[2] + "/" + d[1] + "/" + d[0];
        } catch (Exception e) { return iso; }
    }

    @Override public int getItemCount() { return bookings == null ? 0 : bookings.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCustomerName, tvStatus, tvBookingTime, tvBarberName,
                tvServiceInfo, tvNote, tvCancelReason;
        LinearLayout layoutActions, layoutActionsConfirmed, layoutActionsInProgress;
        Button btnConfirm, btnReject, btnStart, btnComplete, btnNoShow, btnCompleteInProgress;

        ViewHolder(View v) {
            super(v);
            tvCustomerName          = v.findViewById(R.id.tv_customer_name);
            tvStatus                = v.findViewById(R.id.tv_status);
            tvBookingTime           = v.findViewById(R.id.tv_booking_time);
            tvBarberName            = v.findViewById(R.id.tv_barber_name);
            tvServiceInfo           = v.findViewById(R.id.tv_service_info);
            tvNote                  = v.findViewById(R.id.tv_note);
            tvCancelReason          = v.findViewById(R.id.tv_cancel_reason);
            layoutActions           = v.findViewById(R.id.layout_actions);
            layoutActionsConfirmed  = v.findViewById(R.id.layout_actions_confirmed);
            layoutActionsInProgress = v.findViewById(R.id.layout_actions_in_progress);
            btnConfirm              = v.findViewById(R.id.btn_confirm);
            btnReject               = v.findViewById(R.id.btn_reject);
            btnStart                = v.findViewById(R.id.btn_start);
            btnComplete             = v.findViewById(R.id.btn_complete);
            btnNoShow               = v.findViewById(R.id.btn_no_show);
            btnCompleteInProgress   = v.findViewById(R.id.btn_complete_in_progress);
        }
    }
}