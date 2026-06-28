package com.haircut.app.model;

import com.google.gson.annotations.SerializedName;

public class ReviewModel {
    @SerializedName("id")
    public Long id;
    @SerializedName("bookingId")
    public Long bookingId;
    @SerializedName("rating")
    public int rating;
    @SerializedName("comment")
    public String comment;
    @SerializedName("createdAt")
    public String createdAt;
}
