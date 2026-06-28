package com.haircut.app.model;

import com.google.gson.annotations.SerializedName;

public class ReviewRequest {
    @SerializedName("bookingId")
    public Long bookingId;
    @SerializedName("rating")
    public int rating;
    @SerializedName("comment")
    public String comment;
}
