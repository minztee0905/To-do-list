package com.example.ticktok.reminder;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.ticktok.model.Task;


public final class ReminderManager {

    public static final String EXTRA_TASK_ID = "extra_task_id";
    public static final String EXTRA_TASK_TITLE = "extra_task_title";

    public static final int REQUEST_CODE_POST_NOTIFICATIONS = 1001;

    private ReminderManager() {
    }


    public static void ensureNotificationPermission(@NonNull Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return;
        }
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            return;
        }
        ActivityCompat.requestPermissions(
                activity,
                new String[]{Manifest.permission.POST_NOTIFICATIONS},
                REQUEST_CODE_POST_NOTIFICATIONS
        );
    }


    public static void setReminder(@NonNull Context context, @NonNull Task task) {
        Long reminderTime = task.getReminderTime();
        if (reminderTime == null || reminderTime <= System.currentTimeMillis()) {

            if (task.getId() != null) {
                cancelReminder(context, task.getId());
            }
            return;
        }

        String taskId = task.getId();
        if (taskId == null || taskId.trim().isEmpty()) {
            return;
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }

        PendingIntent pendingIntent = buildPendingIntent(context, taskId, task.getTitle());


        alarmManager.cancel(pendingIntent);


        boolean canExact = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            canExact = alarmManager.canScheduleExactAlarms();
        }

        if (canExact) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent);
        } else {

            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent);
        }
    }


    public static void cancelReminder(@NonNull Context context, @NonNull String taskId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }

        PendingIntent pendingIntent = buildPendingIntent(context, taskId, null);
        alarmManager.cancel(pendingIntent);
        pendingIntent.cancel();
    }

    @NonNull
    private static PendingIntent buildPendingIntent(@NonNull Context context,
                                                   @NonNull String taskId,
                                                   @Nullable String taskTitle) {
        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.setAction("com.example.ticktok.ACTION_TASK_REMINDER");
        intent.putExtra(EXTRA_TASK_ID, taskId);
        if (taskTitle != null) {
            intent.putExtra(EXTRA_TASK_TITLE, taskTitle);
        }

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        return PendingIntent.getBroadcast(
                context,
                stableRequestCode(taskId),
                intent,
                flags
        );
    }

    private static int stableRequestCode(@NonNull String taskId) {
        return taskId.hashCode();
    }
}

