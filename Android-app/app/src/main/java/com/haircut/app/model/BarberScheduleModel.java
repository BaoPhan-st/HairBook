package com.haircut.app.model;

import com.google.gson.annotations.SerializedName;

public class BarberScheduleModel {
    @SerializedName("id")        public Long   id;
    @SerializedName("workDate")  public String workDate;
    @SerializedName("startTime") public String startTime;
    @SerializedName("endTime")   public String endTime;
}