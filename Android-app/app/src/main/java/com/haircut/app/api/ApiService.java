package com.haircut.app.api;

import com.haircut.app.admin.AdminUserModel;
import com.haircut.app.admin.DashboardStatsModel;
import com.haircut.app.model.AuthResponse;
import com.haircut.app.model.BarberModel;
import com.haircut.app.model.BarberRequest;
import com.haircut.app.model.BarberScheduleModel;
import com.haircut.app.model.BarberScheduleRequest;
import com.haircut.app.model.BarberBookingModel;
import com.haircut.app.model.BookingModel;
import com.haircut.app.model.BookingRequest;
import com.haircut.app.model.CancelRequest;
import com.haircut.app.model.PaymentRequest;
import com.haircut.app.model.PaymentResponse;
import com.haircut.app.model.RescheduleRequest;
import com.haircut.app.model.ChatRequest;
import com.haircut.app.model.ChatResponse;
import com.haircut.app.model.LoginRequest;
import com.haircut.app.model.RegisterRequest;
import com.haircut.app.model.ReviewModel;
import com.haircut.app.model.ReviewRequest;
import com.haircut.app.model.ServiceModel;
import com.haircut.app.model.UserModel;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.*;

public interface ApiService {

    // ---- AUTH ----
    @POST("auth/login")
    Call<AuthResponse> login(@Body LoginRequest request);

    @POST("auth/register")
    Call<AuthResponse> register(@Body RegisterRequest request);

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
        @Query("date") String date,
        @Query("serviceId") Long serviceId
    );

    @POST("barbers")
    Call<BarberModel> createBarber(@Body BarberRequest body);

    @PUT("barbers/{id}")
    Call<BarberModel> updateBarber(@Path("id") Long id, @Body BarberRequest body);

    @DELETE("barbers/{id}")
    Call<Void> deleteBarber(@Path("id") Long id);

    @GET("barbers/{id}/bookings")
    Call<List<BarberBookingModel>> getBarberBookings(
            @Path("id") Long barberId,
            @Query("date") String date
    );

    // ---- BOOKINGS ----
    @POST("bookings")
    Call<BookingModel> createBooking(@Body BookingRequest request);

    @GET("bookings/my")
    Call<List<BookingModel>> getMyBookings();

    @PUT("bookings/{id}/cancel")
    Call<BookingModel> cancelBooking(@Path("id") Long bookingId, @Body CancelRequest body);

    @PUT("bookings/{id}/reschedule")
    Call<BookingModel> rescheduleBooking(@Path("id") Long bookingId, @Body RescheduleRequest body);

    // ---- BARBER SCHEDULES  ----
    @GET("barbers/{id}/schedules")
    Call<List<BarberScheduleModel>> getBarberSchedules(
            @Path("id") Long barberId,
            @Query("from") String from,
            @Query("to") String to
    );

    @POST("barbers/{id}/schedules")
    Call<BarberScheduleModel> setBarberSchedule(
            @Path("id") Long barberId,
            @Body BarberScheduleRequest body
    );

    @DELETE("barbers/{id}/schedules")
    Call<Void> deleteBarberSchedule(
            @Path("id") Long barberId,
            @Query("date") String date
    );

    // ---- AI CHAT ----
    @POST("ai/chat")
    Call<ChatResponse> sendChatMessage(@Body ChatRequest request);

    // ---- REVIEW ----
    @POST("reviews")
    Call<ReviewModel> submitReview(@Body ReviewRequest request);

    @GET("reviews/booking/{bookingId}")
    Call<ReviewModel> getReviewByBooking(@Path("bookingId") Long bookingId);

    // ---- PAYMENT ----
    @POST("payments/create")
    Call<PaymentResponse> createPayment(@Body PaymentRequest request);

    @GET("payments/status/{orderId}")
    Call<PaymentResponse> getPaymentStatus(@Path("orderId") String orderId);

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
