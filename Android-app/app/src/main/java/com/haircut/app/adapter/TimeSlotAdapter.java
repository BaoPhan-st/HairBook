package com.haircut.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.haircut.app.R;

import java.util.List;

public class TimeSlotAdapter extends RecyclerView.Adapter<TimeSlotAdapter.ViewHolder> {

    public interface OnSlotClick { void onClick(String slot); }

    private final List<String> slots;
    private final OnSlotClick listener;
    private int selectedPosition = -1;

    public TimeSlotAdapter(List<String> slots, OnSlotClick listener) {
        this.slots = slots;
        this.listener = listener;
    }

    public String getSelectedSlot() {
        return selectedPosition >= 0 ? slots.get(selectedPosition) : null;
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_time_slot, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.tvSlot.setText(slots.get(position));
        holder.tvSlot.setSelected(position == selectedPosition);
        holder.tvSlot.setTextColor(position == selectedPosition ? 0xFFFFFFFF : 0xFF7B1FA2);
        holder.itemView.setOnClickListener(v -> {
            int prev = selectedPosition;
            selectedPosition = position;
            notifyItemChanged(prev);
            notifyItemChanged(position);
            listener.onClick(slots.get(position));
        });
    }

    @Override public int getItemCount() { return slots.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSlot;
        ViewHolder(View v) {
            super(v);
            tvSlot = v.findViewById(R.id.tv_time_slot);
        }
    }
}
