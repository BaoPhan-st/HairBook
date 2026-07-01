package com.haircut.app.admin;

import com.google.gson.annotations.SerializedName;

public class DashboardStatsModel {
    @SerializedName("totalUsers")        public long totalUsers;
    @SerializedName("totalCustomers")    public long totalCustomers;
    @SerializedName("totalAdmins")       public long totalAdmins;
    @SerializedName("totalBookings")     public long totalBookings;
    @SerializedName("pendingBookings")   public long pendingBookings;
    @SerializedName("confirmedBookings") public long confirmedBookings;
    @SerializedName("completedBookings") public long completedBookings;
    @SerializedName("cancelledBookings") public long cancelledBookings;
    @SerializedName("todayBookings")     public long todayBookings;
}
