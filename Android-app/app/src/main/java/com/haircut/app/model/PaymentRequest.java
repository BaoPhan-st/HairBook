package com.haircut.app.model;

import com.google.gson.annotations.SerializedName;

public class PaymentRequest {
    @SerializedName("bookingId")
    public Long bookingId;
    @SerializedName("method")
    public String method;
    @SerializedName("returnUrl")
    public String returnUrl;
}
