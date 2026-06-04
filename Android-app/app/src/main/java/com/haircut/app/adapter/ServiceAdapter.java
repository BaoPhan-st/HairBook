package com.haircut.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.haircut.app.R;
import com.haircut.app.model.ServiceModel;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class ServiceAdapter extends RecyclerView.Adapter<ServiceAdapter.ViewHolder> {

    public interface OnServiceClick { void onClick(ServiceModel service); }

    private final List<ServiceModel> services;
    private final OnServiceClick listener;

    public ServiceAdapter(List<ServiceModel> services, OnServiceClick listener) {
        this.services = services;
        this.listener = listener;
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_service, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ServiceModel s = services.get(position);
        holder.tvName.setText(s.name);
        if (s.price != null) {
            NumberFormat fmt = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
            holder.tvPrice.setText(fmt.format(s.price.longValue()) + "đ");
        }
        if (s.durationMinutes != null) {
            holder.tvDuration.setText(s.durationMinutes + " phút");
        }
        holder.itemView.setOnClickListener(v -> listener.onClick(s));
    }

    @Override public int getItemCount() { return services.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvPrice, tvDuration;
        ViewHolder(View v) {
            super(v);
            tvName     = v.findViewById(R.id.tv_service_name);
            tvPrice    = v.findViewById(R.id.tv_service_price);
            tvDuration = v.findViewById(R.id.tv_service_duration);
        }
    }
}
