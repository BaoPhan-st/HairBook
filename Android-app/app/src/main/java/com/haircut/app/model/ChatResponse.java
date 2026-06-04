package com.haircut.app.model;
import com.google.gson.annotations.SerializedName;
public class ChatResponse {
    @SerializedName("reply") public String reply;
    @SerializedName("fromAI") public boolean fromAI;
}
