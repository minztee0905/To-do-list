package com.example.ticktok.reminder;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.ticktok.R;
import com.example.ticktok.activity.MainActivity;

public class NotificationReceiver extends BroadcastReceiver {

    public static final String CHANNEL_ID_TASK_REMINDER = "task_reminder";
    public static final String CHANNEL_ID_EVENT_REMINDER = "event_reminder";
    private static final String CHANNEL_NAME = "Task reminders";
    private static final String CHANNEL_NAME_EVENT = "Event reminders";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) {
            return;
        }

        String action = intent.getAction();
        if (EventReminderManager.ACTION_EVENT_REMINDER.equals(action)) {
            showEventReminder(context, intent);
            return;
        }


        showTaskReminder(context, intent);
    }

    private void showTaskReminder(@NonNull Context context, @NonNull Intent intent) {

        String taskId = intent.getStringExtra(ReminderManager.EXTRA_TASK_ID);
        String taskTitle = intent.getStringExtra(ReminderManager.EXTRA_TASK_TITLE);
        if (taskTitle == null || taskTitle.trim().isEmpty()) {
            taskTitle = context.getString(R.string.app_name);
        }

        ensureTaskChannel(context);

        Intent openIntent = new Intent(context, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if (taskId != null) {
            openIntent.putExtra(ReminderManager.EXTRA_TASK_ID, taskId);
        }

        int piFlags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;

        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                taskId != null ? taskId.hashCode() : 0,
                openIntent,
                piFlags
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_TASK_REMINDER)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(taskTitle)
                .setContentText(context.getString(R.string.notification_task_reminder_body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(contentIntent);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        try {
            NotificationManagerCompat.from(context)
                    .notify(taskId != null ? taskId.hashCode() : (int) System.currentTimeMillis(), builder.build());
        } catch (SecurityException ignored) {

        }
    }

    private void showEventReminder(@NonNull Context context, @NonNull Intent intent) {
        String eventId = intent.getStringExtra(EventReminderManager.EXTRA_EVENT_ID);
        String eventTitle = intent.getStringExtra(EventReminderManager.EXTRA_EVENT_TITLE);
        String type = intent.getStringExtra(EventReminderManager.EXTRA_EVENT_TYPE);
        if (eventTitle == null || eventTitle.trim().isEmpty()) {
            eventTitle = context.getString(R.string.app_name);
        }

        ensureEventChannel(context);

        Intent openIntent = new Intent(context, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        int piFlags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;

        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                (eventId != null ? (eventId + "_" + type).hashCode() : (int) System.currentTimeMillis()),
                openIntent,
                piFlags
        );

        String contentText;
        if (EventReminderManager.TYPE_ONE_DAY_BEFORE.equals(type)) {
            contentText = context.getString(R.string.notification_event_reminder_body_one_day_before);
        } else {
            contentText = context.getString(R.string.notification_event_reminder_body_day_of);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_EVENT_REMINDER)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(eventTitle)
                .setContentText(contentText)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(contentIntent);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        try {
            int notifyId = eventId != null
                    ? (eventId + "_" + type).hashCode()
                    : (int) System.currentTimeMillis();
            NotificationManagerCompat.from(context).notify(notifyId, builder.build());
        } catch (SecurityException ignored) {
        }
    }

    private void ensureTaskChannel(@NonNull Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) {
            return;
        }

        NotificationChannel existing = manager.getNotificationChannel(CHANNEL_ID_TASK_REMINDER);
        if (existing != null) {
            return;
        }

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID_TASK_REMINDER,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription(context.getString(R.string.notification_task_reminder_channel_description));
        manager.createNotificationChannel(channel);
    }

    private void ensureEventChannel(@NonNull Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) {
            return;
        }

        NotificationChannel existing = manager.getNotificationChannel(CHANNEL_ID_EVENT_REMINDER);
        if (existing != null) {
            return;
        }

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID_EVENT_REMINDER,
                CHANNEL_NAME_EVENT,
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription(context.getString(R.string.notification_event_reminder_channel_description));
        manager.createNotificationChannel(channel);
    }
}


