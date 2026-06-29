package com.haircut.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.haircut.app.R;
import com.haircut.app.model.BarberModel;

import java.util.List;

public class BarberAdapter extends RecyclerView.Adapter<BarberAdapter.ViewHolder> {

    public interface OnBarberActionListener {
        default void onEdit(BarberModel barber) {}
        default void onDelete(BarberModel barber) {}
        void onSchedule(BarberModel barber);
    }

    private final List<BarberModel> items;
    private final OnBarberActionListener listener;
    private final int layoutId;

    public BarberAdapter(List<BarberModel> items, OnBarberActionListener listener) {
        this(items, R.layout.item_barber, listener);
    }

    public BarberAdapter(List<BarberModel> items, int layoutId, OnBarberActionListener listener) {
        this.items = items;
        this.layoutId = layoutId;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(layoutId, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        BarberModel b = items.get(position);
        if (h.tvName != null) h.tvName.setText(b.name);
        if (h.tvSpecialty != null) h.tvSpecialty.setText(b.specialty);
        if (h.tvRating != null) h.tvRating.setText("⭐ " + (b.rating != null ? b.rating : "—"));
        if (h.tvStatus != null) {
            h.tvStatus.setText(Boolean.TRUE.equals(b.available) ? "🟢 Đang làm" : "🔴 Nghỉ");
        }

        if (h.imgBarber != null && b.imageUrl != null) {
            Glide.with(h.itemView.getContext())
                    .load(b.imageUrl)
                    .placeholder(android.R.drawable.ic_menu_myplaces)
                    .into(h.imgBarber);
        }

        if (h.btnEdit != null) h.btnEdit.setOnClickListener(v -> listener.onEdit(b));
        if (h.btnDelete != null) h.btnDelete.setOnClickListener(v -> listener.onDelete(b));
        if (h.btnSchedule != null) h.btnSchedule.setOnClickListener(v -> listener.onSchedule(b));

        // Cho phép click toàn bộ item để kích hoạt hành động chính (onSchedule)
        h.itemView.setOnClickListener(v -> listener.onSchedule(b));
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvSpecialty, tvRating, tvStatus;
        ImageButton btnEdit, btnDelete, btnSchedule;
        ImageView imgBarber;

        ViewHolder(View v) {
            super(v);
            tvName      = v.findViewById(R.id.tvBarberName);
            if (tvName == null) tvName = v.findViewById(R.id.tv_barber_name);

            tvSpecialty = v.findViewById(R.id.tvSpecialty);
            if (tvSpecialty == null) tvSpecialty = v.findViewById(R.id.tv_barber_specialty);

            tvRating    = v.findViewById(R.id.tvRating);
            if (tvRating == null) tvRating = v.findViewById(R.id.tv_barber_rating);

            tvStatus    = v.findViewById(R.id.tvStatus);
            if (tvStatus == null) tvStatus = v.findViewById(R.id.tv_barber_available);

            btnEdit     = v.findViewById(R.id.btnEdit);
            btnDelete   = v.findViewById(R.id.btnDelete);
            btnSchedule = v.findViewById(R.id.btnSchedule);
            
            imgBarber   = v.findViewById(R.id.img_barber);
        }
    }
}
