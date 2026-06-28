package com.haircut.app.notification;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ReminderScheduler {

    private static final long ONE_HOUR_MS = 60 * 60 * 1000L;
    private static final long FIFTEEN_MIN_MS = 15 * 60 * 1000L;

    public static void schedule(Context context, long bookingId,
                                String bookingTimeIso,
                                String barberName, String serviceName) {
        long appointmentMs = parseIso(bookingTimeIso);
        if (appointmentMs <= 0) return;

        String timeDisplay = formatDisplay(bookingTimeIso);
        String title = "✂️ Nhắc lịch cắt tóc – " + serviceName;

        long remind1h = appointmentMs - ONE_HOUR_MS;
        if (remind1h > System.currentTimeMillis()) {
            setAlarm(context,
                    (int) (bookingId * 10),
                    remind1h,
                    title,
                    "Còn 1 giờ nữa đến lịch với " + barberName + " lúc " + timeDisplay);
        }

        long remind15m = appointmentMs - FIFTEEN_MIN_MS;
        if (remind15m > System.currentTimeMillis()) {
            setAlarm(context,
                    (int) (bookingId * 10 + 1),
                    remind15m,
                    title,
                    "Còn 15 phút! Hãy chuẩn bị đến gặp " + barberName + " 🕐");
        }
    }

    public static void cancel(Context context, long bookingId) {
        cancelAlarm(context, (int) (bookingId * 10));
        cancelAlarm(context, (int) (bookingId * 10 + 1));
    }

    private static void setAlarm(Context context, int requestCode,
                                 long triggerAtMs, String title, String body) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        PendingIntent pi = buildPendingIntent(context, requestCode, title, body);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
            am.set(AlarmManager.RTC_WAKEUP, triggerAtMs, pi);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pi);
        } else {
            am.setExact(AlarmManager.RTC_WAKEUP, triggerAtMs, pi);
        }
    }

    private static void cancelAlarm(Context context, int requestCode) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        PendingIntent pi = buildPendingIntent(context, requestCode, "", "");
        am.cancel(pi);
    }

    private static PendingIntent buildPendingIntent(Context context, int requestCode,
                                                    String title, String body) {
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra(ReminderReceiver.EXTRA_TITLE, title);
        intent.putExtra(ReminderReceiver.EXTRA_BODY, body);
        intent.putExtra(ReminderReceiver.EXTRA_NOTIF_ID, requestCode);
        return PendingIntent.getBroadcast(
                context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static long parseIso(String iso) {
        try {
            SimpleDateFormat sdf =
                    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            Date d = sdf.parse(iso);
            return d != null ? d.getTime() : -1;
        } catch (Exception e) {
            return -1;
        }
    }

    private static String formatDisplay(String iso) {
        try {
            String[] parts = iso.split("T");
            String[] date = parts[0].split("-");
            String time = parts[1].substring(0, 5);
            return time + " ngày " + date[2] + "/" + date[1];
        } catch (Exception e) {
            return iso;
        }
    }
}
