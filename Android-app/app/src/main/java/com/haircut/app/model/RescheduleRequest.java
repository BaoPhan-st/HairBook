package com.haircut.app.model;

import com.google.gson.annotations.SerializedName;

public class RescheduleRequest {
    @SerializedName("newBookingTime")
    public String newBookingTime;

    public RescheduleRequest() {}

    public RescheduleRequest(String newBookingTime) {
        this.newBookingTime = newBookingTime;
    }
}
