package com.haircut.app.admin;

import com.google.gson.annotations.SerializedName;

public class AdminUserModel {
    @SerializedName("id")        public Long   id;
    @SerializedName("email")     public String email;
    @SerializedName("fullName")  public String fullName;
    @SerializedName("phone")     public String phone;
    @SerializedName("role")      public String role;
    @SerializedName("status")    public String status;
    @SerializedName("createdAt") public String createdAt;

    /** Trả về ký tự đầu của tên để dùng làm avatar placeholder */
    public String getInitial() {
        if (fullName != null && !fullName.isEmpty()) {
            return String.valueOf(fullName.charAt(0)).toUpperCase();
        }
        return "?";
    }

    public boolean isAdmin()  { return "ADMIN".equalsIgnoreCase(role); }
    public boolean isLocked() { return "LOCKED".equalsIgnoreCase(status); }
}
