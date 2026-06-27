package com.haircut.app.api;

import com.haircut.app.admin.AdminUserModel;
import com.haircut.app.admin.DashboardStatsModel;
import com.haircut.app.model.AuthResponse;
import com.haircut.app.model.BarberModel;
import com.haircut.app.model.BookingModel;
import com.haircut.app.model.BookingRequest;
import com.haircut.app.model.ChatRequest;
import com.haircut.app.model.ChatResponse;
import com.haircut.app.model.LoginRequest;
import com.haircut.app.model.RegisterRequest;
import com.haircut.app.model.ServiceModel;
import com.haircut.app.model.UserModel;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.*;

public interface ApiService {

    // ── AUTH ──────────────────────────────────────────────────────────────────
    @POST("auth/login")
    Call<AuthResponse> login(@Body LoginRequest request);

    @POST("auth/register")
    Call<AuthResponse> register(@Body RegisterRequest request);

    @GET("auth/me")
    Call<UserModel> getCurrentUser();

    // ── SERVICES ──────────────────────────────────────────────────────────────
    @GET("services")
    Call<List<ServiceModel>> getAllServices();

    // ── BARBERS ───────────────────────────────────────────────────────────────
    @GET("barbers")
    Call<List<BarberModel>> getAllBarbers();

    @GET("barbers/{id}/available-slots")
    Call<List<String>> getAvailableSlots(
        @Path("id") Long barberId,
        @Query("date") String date
    );

    // ── BOOKINGS ──────────────────────────────────────────────────────────────
    @POST("bookings")
    Call<BookingModel> createBooking(@Body BookingRequest request);

    @GET("bookings/my")
    Call<List<BookingModel>> getMyBookings();

    @PUT("bookings/{id}/cancel")
    Call<BookingModel> cancelBooking(@Path("id") Long bookingId);

    // ── AI CHAT ───────────────────────────────────────────────────────────────
    @POST("ai/chat")
    Call<ChatResponse> sendChatMessage(@Body ChatRequest request);

    // ── ADMIN: Dashboard ──────────────────────────────────────────────────────
    @GET("admin/dashboard")
    Call<DashboardStatsModel> getDashboardStats();

    // ── ADMIN: Users ──────────────────────────────────────────────────────────
    @GET("admin/users")
    Call<List<AdminUserModel>> getAllUsers();

    @GET("admin/users/search")
    Call<List<AdminUserModel>> searchUsers(@Query("q") String query);

    @GET("admin/users/{id}")
    Call<AdminUserModel> getUserDetail(@Path("id") Long userId);

    @PUT("admin/users/{id}/role")
    Call<AdminUserModel> updateUserRole(
        @Path("id") Long userId,
        @Body Map<String, String> body
    );

    @PUT("admin/users/{id}/status")
    Call<AdminUserModel> updateUserStatus(
        @Path("id") Long userId,
        @Body Map<String, String> body
    );
}
