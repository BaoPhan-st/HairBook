package com.haircut.app.model;
import com.google.gson.annotations.SerializedName;
public class UserModel {
    @SerializedName("id") public Long id;
    @SerializedName("email") public String email;
    @SerializedName("fullName") public String fullName;
    @SerializedName("phone") public String phone;
}
