package com.haircut.app.model;

import com.google.gson.annotations.SerializedName;

public class BarberRequest {
    @SerializedName("name")      public String name;
    @SerializedName("specialty") public String specialty;
    @SerializedName("imageUrl")  public String imageUrl;
    @SerializedName("rating")    public Double rating;
    @SerializedName("available") public Boolean available;

    public BarberRequest(String name, String specialty, String imageUrl, Double rating, Boolean available) {
        this.name = name;
        this.specialty = specialty;
        this.imageUrl = imageUrl;
        this.rating = rating;
        this.available = available;
    }
}