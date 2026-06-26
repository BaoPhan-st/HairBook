package com.haircut.app.model;

import com.google.gson.annotations.SerializedName;

public class BarberBookingModel {
    @SerializedName("bookingId")   public Long bookingId;
    @SerializedName("bookingTime") public String bookingTime;
    @SerializedName("status")      public String status;
    @SerializedName("note")        public String note;
    @SerializedName("customer")    public CustomerInfo customer;
    @SerializedName("service")     public ServiceInfo service;

    public static class CustomerInfo {
        @SerializedName("id")       public Long id;
        @SerializedName("fullName") public String fullName;
        @SerializedName("phone")    public String phone;
    }

    public static class ServiceInfo {
        @SerializedName("id")    public Long id;
        @SerializedName("name")  public String name;
        @SerializedName("price") public Double price;
    }
}