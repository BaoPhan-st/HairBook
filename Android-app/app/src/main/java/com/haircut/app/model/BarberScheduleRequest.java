package com.haircut.app.model;

import com.google.gson.annotations.SerializedName;

public class BarberScheduleRequest {
    @SerializedName("workDate")  public String workDate;
    @SerializedName("startTime") public String startTime;
    @SerializedName("endTime")   public String endTime;

    public BarberScheduleRequest(String workDate, String startTime, String endTime) {
        this.workDate  = workDate;
        this.startTime = startTime;
        this.endTime   = endTime;
    }
}