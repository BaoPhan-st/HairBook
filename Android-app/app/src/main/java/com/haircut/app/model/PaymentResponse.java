package com.haircut.app.model;

import com.google.gson.annotations.SerializedName;
public class PaymentResponse {
    @SerializedName("paymentUrl")
    public String paymentUrl;
    @SerializedName("orderId")
    public String orderId;
    @SerializedName("amount")
    public Double amount;
    @SerializedName("method")
    public String method;
    @SerializedName("status")
    public String status;
}
