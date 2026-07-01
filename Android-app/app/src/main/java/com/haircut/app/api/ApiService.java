package com.haircut.app.api;

import com.haircut.app.model.AuthResponse;
import com.haircut.app.model.BarberModel;
import com.haircut.app.model.BookingModel;
import com.haircut.app.model.BookingRequest;
import com.haircut.app.model.ChatRequest;
import com.haircut.app.model.ChatResponse;
import com.haircut.app.model.GoogleLoginRequest;
import com.haircut.app.model.LoginRequest;
import com.haircut.app.model.RegisterRequest;
import com.haircut.app.model.ServiceModel;
import com.haircut.app.model.UserModel;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.*;

public interface ApiService {

    // ---- AUTH ----
    @POST("auth/login")
    Call<AuthResponse> login(@Body LoginRequest request);

    @POST("auth/register")
    Call<AuthResponse> register(@Body RegisterRequest request);

    @POST("auth/google")
    Call<AuthResponse> googleLogin(@Body GoogleLoginRequest request);

    @GET("auth/me")
    Call<UserModel> getCurrentUser();


    // ---- SERVICES ----
    @GET("services")
    Call<List<ServiceModel>> getAllServices();

    // ---- BARBERS ----
    @GET("barbers")
    Call<List<BarberModel>> getAllBarbers();

    @GET("barbers/{id}/available-slots")
    Call<List<String>> getAvailableSlots(
        @Path("id") Long barberId,
        @Query("date") String date
    );

    // ---- BOOKINGS ----
    @POST("bookings")
    Call<BookingModel> createBooking(@Body BookingRequest request);

    @GET("bookings/my")
    Call<List<BookingModel>> getMyBookings();

    @PUT("bookings/{id}/cancel")
    Call<BookingModel> cancelBooking(@Path("id") Long bookingId);

    // ---- AI CHAT ----
    @POST("ai/chat")
    Call<ChatResponse> sendChatMessage(@Body ChatRequest request);
}
