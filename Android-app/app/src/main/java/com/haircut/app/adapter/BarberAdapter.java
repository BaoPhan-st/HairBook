package com.haircut.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.haircut.app.R;
import com.haircut.app.model.BarberModel;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class BarberAdapter extends RecyclerView.Adapter<BarberAdapter.ViewHolder> {

    public interface OnBarberClick { void onClick(BarberModel barber); }

    private final List<BarberModel> barbers;
    private final OnBarberClick listener;

    public BarberAdapter(List<BarberModel> barbers, OnBarberClick listener) {
        this.barbers = barbers;
        this.listener = listener;
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_barber, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BarberModel b = barbers.get(position);
        holder.tvName.setText(b.name);
        holder.tvSpecialty.setText(b.specialty);
        if (b.rating != null) holder.tvRating.setText(String.format("%.1f", b.rating));
        if (b.available != null) {
            holder.tvAvailable.setText(b.available ? "Còn trống" : "Đã đầy lịch");
            holder.tvAvailable.setTextColor(b.available ? 0xFF4CAF50 : 0xFFF44336);
        }
        if (b.imageUrl != null && !b.imageUrl.isEmpty()) {
            Glide.with(holder.itemView).load(b.imageUrl).into(holder.imgBarber);
        }
        holder.itemView.setOnClickListener(v -> listener.onClick(b));
    }

    @Override public int getItemCount() { return barbers.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CircleImageView imgBarber;
        TextView tvName, tvSpecialty, tvRating, tvAvailable;
        ViewHolder(View v) {
            super(v);
            imgBarber   = v.findViewById(R.id.img_barber);
            tvName      = v.findViewById(R.id.tv_barber_name);
            tvSpecialty = v.findViewById(R.id.tv_barber_specialty);
            tvRating    = v.findViewById(R.id.tv_barber_rating);
            tvAvailable = v.findViewById(R.id.tv_barber_available);
        }
    }
}
