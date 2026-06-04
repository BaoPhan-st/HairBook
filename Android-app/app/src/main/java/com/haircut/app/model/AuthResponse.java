package com.haircut.app.model;
import com.google.gson.annotations.SerializedName;
public class AuthResponse {
    @SerializedName("token") public String token;
    @SerializedName("email") public String email;
    @SerializedName("fullName") public String fullName;
}
