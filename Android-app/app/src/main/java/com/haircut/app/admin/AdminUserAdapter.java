package com.haircut.app.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.haircut.app.R;

import java.util.List;

public class AdminUserAdapter extends RecyclerView.Adapter<AdminUserAdapter.ViewHolder> {

    public interface OnUserClickListener {
        void onUserClick(AdminUserModel user);
    }

    private List<AdminUserModel> users;
    private final OnUserClickListener listener;

    public AdminUserAdapter(List<AdminUserModel> users, OnUserClickListener listener) {
        this.users    = users;
        this.listener = listener;
    }

    public void updateList(List<AdminUserModel> newList) {
        this.users = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(users.get(position), listener);
    }

    @Override
    public int getItemCount() { return users.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvInitial, tvFullName, tvEmail, tvPhone, tvRole, tvStatus;

        ViewHolder(View itemView) {
            super(itemView);
            tvInitial  = itemView.findViewById(R.id.tv_initial);
            tvFullName = itemView.findViewById(R.id.tv_full_name);
            tvEmail    = itemView.findViewById(R.id.tv_email);
            tvPhone    = itemView.findViewById(R.id.tv_phone);
            tvRole     = itemView.findViewById(R.id.tv_role);
            tvStatus   = itemView.findViewById(R.id.tv_status);
        }

        void bind(AdminUserModel user, OnUserClickListener listener) {
            tvInitial.setText(user.getInitial());
            tvFullName.setText(user.fullName != null ? user.fullName : "-");
            tvEmail.setText(user.email != null ? user.email : "-");
            tvPhone.setText((user.phone != null && !user.phone.isEmpty()) ? user.phone : "Chưa có SĐT");

            if (user.isAdmin()) {
                tvRole.setText("ADMIN");
                tvRole.setBackgroundResource(R.drawable.badge_admin);
            } else {
                tvRole.setText("CUSTOMER");
                tvRole.setBackgroundResource(R.drawable.badge_customer);
            }

            if (user.isLocked()) {
                tvStatus.setText("LOCKED");
                tvStatus.setBackgroundResource(R.drawable.badge_locked);
            } else {
                tvStatus.setText("ACTIVE");
                tvStatus.setBackgroundResource(R.drawable.badge_active);
            }

            itemView.setOnClickListener(v -> listener.onUserClick(user));
        }
    }
}
