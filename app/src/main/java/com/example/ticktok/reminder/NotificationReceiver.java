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
    private static final String CHANNEL_NAME = "Task reminders";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) {
            return;
        }

        String taskId = intent.getStringExtra(ReminderManager.EXTRA_TASK_ID);
        String taskTitle = intent.getStringExtra(ReminderManager.EXTRA_TASK_TITLE);
        if (taskTitle == null || taskTitle.trim().isEmpty()) {
            taskTitle = context.getString(R.string.app_name);
        }

        ensureChannel(context);

        Intent openIntent = new Intent(context, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if (taskId != null) {
            openIntent.putExtra(ReminderManager.EXTRA_TASK_ID, taskId);
        }

        int piFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            piFlags |= PendingIntent.FLAG_IMMUTABLE;
        }

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
            // Permission can still be revoked by user at runtime.
        }
    }

    private void ensureChannel(@NonNull Context context) {
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
}


