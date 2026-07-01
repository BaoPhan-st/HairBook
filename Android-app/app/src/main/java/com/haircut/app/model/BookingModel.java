package com.haircut.app.model;
import com.google.gson.annotations.SerializedName;
public class BookingModel {
    @SerializedName("id") public Long id;
    @SerializedName("barber") public BarberModel barber;
    @SerializedName("service") public ServiceModel service;
    @SerializedName("bookingTime") public String bookingTime;
    @SerializedName("bookingEndTime") public String bookingEndTime;
    @SerializedName("status") public String status;
    @SerializedName("note") public String note;
    @SerializedName("createdAt") public String createdAt;
    @SerializedName("updatedAt") public String updatedAt;
    @SerializedName("cancelledAt") public String cancelledAt;
    @SerializedName("cancelReason") public String cancelReason;
}