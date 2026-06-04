package com.haircut.app.model;
import com.google.gson.annotations.SerializedName;
public class RegisterRequest {
    @SerializedName("email") public String email;
    @SerializedName("password") public String password;
    @SerializedName("fullName") public String fullName;
    @SerializedName("phone") public String phone;
}
