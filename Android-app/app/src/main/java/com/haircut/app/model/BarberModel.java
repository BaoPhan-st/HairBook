package com.haircut.app.model;
import com.google.gson.annotations.SerializedName;
public class BarberModel {
    @SerializedName("id") public Long id;
    @SerializedName("name") public String name;
    @SerializedName("specialty") public String specialty;
    @SerializedName("imageUrl") public String imageUrl;
    @SerializedName("rating") public Double rating;
    @SerializedName("available") public Boolean available;
}
