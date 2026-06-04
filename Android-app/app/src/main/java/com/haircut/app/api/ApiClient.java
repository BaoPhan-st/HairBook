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
    private static final String PREF_NAME = "haircut_prefs";
    private static final String KEY_TOKEN = "jwt_token";

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


    public static void saveToken(Context context, String token) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_TOKEN, token).apply();
        retrofit = null; // Reset để dùng token mới
    }

    public static String getToken(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_TOKEN, null);
    }

    public static void clearToken(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_TOKEN).apply();
        retrofit = null;
    }

    public static boolean isLoggedIn(Context context) {
        return getToken(context) != null;
    }
}
