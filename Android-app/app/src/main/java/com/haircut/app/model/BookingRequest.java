package com.haircut.app.model;
import com.google.gson.annotations.SerializedName;
public class BookingRequest {
    @SerializedName("barberId") public Long barberId;
    @SerializedName("serviceId") public Long serviceId;
    @SerializedName("bookingTime") public String bookingTime;
    @SerializedName("note") public String note;
}
