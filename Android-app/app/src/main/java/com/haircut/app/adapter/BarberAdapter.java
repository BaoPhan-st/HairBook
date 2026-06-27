package com.haircut.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.haircut.app.R;
import com.haircut.app.model.BarberModel;

import java.util.List;

public class BarberAdapter extends RecyclerView.Adapter<BarberAdapter.ViewHolder> {

    public interface OnBarberActionListener {
        void onEdit(BarberModel barber);
        void onDelete(BarberModel barber);
        void onSchedule(BarberModel barber);
    }

    private final List<BarberModel> items;
    private final OnBarberActionListener listener;

    public BarberAdapter(List<BarberModel> items, OnBarberActionListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_barber, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        BarberModel b = items.get(position);
        h.tvName.setText(b.name);
        h.tvSpecialty.setText(b.specialty);
        h.tvRating.setText("⭐ " + (b.rating != null ? b.rating : "—"));
        h.tvStatus.setText(Boolean.TRUE.equals(b.available) ? "🟢 Đang làm" : "🔴 Nghỉ");
        h.btnEdit.setOnClickListener(v -> listener.onEdit(b));
        h.btnDelete.setOnClickListener(v -> listener.onDelete(b));
        h.btnSchedule.setOnClickListener(v -> listener.onSchedule(b));
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvSpecialty, tvRating, tvStatus;
        ImageButton btnEdit, btnDelete, btnSchedule;

        ViewHolder(View v) {
            super(v);
            tvName      = v.findViewById(R.id.tvBarberName);
            tvSpecialty = v.findViewById(R.id.tvSpecialty);
            tvRating    = v.findViewById(R.id.tvRating);
            tvStatus    = v.findViewById(R.id.tvStatus);
            btnEdit     = v.findViewById(R.id.btnEdit);
            btnDelete   = v.findViewById(R.id.btnDelete);
            btnSchedule = v.findViewById(R.id.btnSchedule);
        }
    }
}