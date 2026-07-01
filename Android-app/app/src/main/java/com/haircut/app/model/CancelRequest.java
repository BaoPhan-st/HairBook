package com.haircut.app.model;
import com.google.gson.annotations.SerializedName;
public class CancelRequest {
    @SerializedName("reason") public String reason;

    public CancelRequest() {}
    public CancelRequest(String reason) {
        this.reason = reason;
    }
}