package com.haircut.app.model;
import com.google.gson.annotations.SerializedName;
public class ServiceModel {
    @SerializedName("id") public Long id;
    @SerializedName("name") public String name;
    @SerializedName("description") public String description;
    @SerializedName("price") public Double price;
    @SerializedName("durationMinutes") public Integer durationMinutes;
    @SerializedName("imageUrl") public String imageUrl;
}
