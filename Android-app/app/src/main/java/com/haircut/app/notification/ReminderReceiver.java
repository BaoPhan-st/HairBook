package com.haircut.app.notification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ReminderReceiver extends BroadcastReceiver {

    public static final String EXTRA_TITLE = "reminder_title";
    public static final String EXTRA_BODY = "reminder_body";
    public static final String EXTRA_NOTIF_ID = "reminder_notif_id";

    @Override
    public void onReceive(Context context, Intent intent) {
        String title = intent.getStringExtra(EXTRA_TITLE);
        String body = intent.getStringExtra(EXTRA_BODY);
        int notifId = intent.getIntExtra(EXTRA_NOTIF_ID,
                NotificationHelper.NOTIF_ID);

        NotificationHelper.createChannel(context);
        NotificationHelper.showNotification(context, title, body, notifId);
    }
}
