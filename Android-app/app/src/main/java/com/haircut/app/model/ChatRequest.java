package com.haircut.app.model;
import com.google.gson.annotations.SerializedName;
public class ChatRequest {
    @SerializedName("message") public String message;
    public ChatRequest(String message) { this.message = message; }
}
