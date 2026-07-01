package com.haircut.app.api;

import android.content.Context;
import android.content.SharedPreferences;

import com.haircut.app.BuildConfig;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.concurrent.TimeUnit;

public class ApiClient {

    private static Retrofit retrofit = null;
    private static final String PREF_NAME  = "haircut_prefs";
    private static final String KEY_TOKEN  = "jwt_token";
    private static final String KEY_ROLE   = "user_role";
    private static final String KEY_NAME   = "user_full_name";
    private static final String KEY_EMAIL  = "user_email";

    public static ApiService getService(Context context) {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BuildConfig.BASE_URL)
                    .client(buildHttpClient(context))
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(ApiService.class);
    }

    private static OkHttpClient buildHttpClient(Context context) {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        Interceptor authInterceptor = chain -> {
            Request original = chain.request();
            String token = getToken(context);
            if (token != null && !token.isEmpty()) {
                original = original.newBuilder()
                        .header("Authorization", "Bearer " + token)
                        .build();
            }
            return chain.proceed(original);
        };

        return new OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .addInterceptor(logging)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    // ─── Token ────────────────────────────────────────────────────────────────

    public static void saveToken(Context context, String token) {
        getPrefs(context).edit().putString(KEY_TOKEN, token).apply();
        retrofit = null;
    }

    public static String getToken(Context context) {
        return getPrefs(context).getString(KEY_TOKEN, null);
    }

    public static void clearToken(Context context) {
        getPrefs(context).edit().remove(KEY_TOKEN).apply();
        retrofit = null;
    }

    public static boolean isLoggedIn(Context context) {
        return getToken(context) != null;
    }

    // ─── User Info (role, fullName, email) ────────────────────────────────────

    public static void saveUserInfo(Context context, String token, String role, String fullName, String email) {
        getPrefs(context).edit()
                .putString(KEY_TOKEN, token)
                .putString(KEY_ROLE, role)
                .putString(KEY_NAME, fullName)
                .putString(KEY_EMAIL, email)
                .apply();
        retrofit = null;
    }

    public static String getRole(Context context) {
        return getPrefs(context).getString(KEY_ROLE, "CUSTOMER");
    }

    public static String getUserName(Context context) {
        return getPrefs(context).getString(KEY_NAME, "");
    }

    public static String getUserEmail(Context context) {
        return getPrefs(context).getString(KEY_EMAIL, "");
    }

    public static boolean isAdmin(Context context) {
        return "ADMIN".equalsIgnoreCase(getRole(context));
    }

    // ─── Logout: xoá toàn bộ user session ────────────────────────────────────

    public static void clearAll(Context context) {
        getPrefs(context).edit().clear().apply();
        retrofit = null;
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
}
