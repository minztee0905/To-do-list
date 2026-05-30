package com.example.ticktok.reminder;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.ticktok.model.Event;

import java.util.Calendar;


public final class EventReminderManager {

    public static final String ACTION_EVENT_REMINDER = "com.example.ticktok.ACTION_EVENT_REMINDER";

    public static final String EXTRA_EVENT_ID = "extra_event_id";
    public static final String EXTRA_EVENT_TITLE = "extra_event_title";
    public static final String EXTRA_EVENT_TYPE = "extra_event_type";

    public static final String TYPE_ONE_DAY_BEFORE = "one_day_before";
    public static final String TYPE_DAY_OF = "day_of";

    private static final int REMIND_HOUR = 8;

    private EventReminderManager() {
    }

    public static void setEventReminders(@NonNull Context context, @NonNull Event event) {
        String eventId = safeTrim(event.getId());
        if (eventId == null) {
            return;
        }

        Long targetDate = event.getTargetDate();
        if (targetDate == null || targetDate <= 0L) {
            cancelEventReminders(context, eventId);
            return;
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }

        // Replace any existing alarms for this event.
        cancelEventReminders(context, eventId);

        String title = event.getTitle();
        long remindOneDayBefore = buildReminderAtMillis(targetDate, -1);
        long remindDayOf = buildReminderAtMillis(targetDate, 0);

        scheduleIfFuture(context, alarmManager, eventId, title, TYPE_ONE_DAY_BEFORE, remindOneDayBefore);
        scheduleIfFuture(context, alarmManager, eventId, title, TYPE_DAY_OF, remindDayOf);
    }

    public static void cancelEventReminders(@NonNull Context context, @NonNull String eventId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }

        PendingIntent piBefore = buildPendingIntent(context, eventId, null, TYPE_ONE_DAY_BEFORE);
        PendingIntent piDayOf = buildPendingIntent(context, eventId, null, TYPE_DAY_OF);

        alarmManager.cancel(piBefore);
        alarmManager.cancel(piDayOf);

        piBefore.cancel();
        piDayOf.cancel();
    }

    private static void scheduleIfFuture(@NonNull Context context,
                                        @NonNull AlarmManager alarmManager,
                                        @NonNull String eventId,
                                        @Nullable String title,
                                        @NonNull String type,
                                        long triggerAtMillis) {
        if (triggerAtMillis <= System.currentTimeMillis()) {
            return;
        }

        PendingIntent pi = buildPendingIntent(context, eventId, title, type);

        boolean canExact = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            canExact = alarmManager.canScheduleExactAlarms();
        }

        if (canExact) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi);
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi);
        }
    }


    private static long buildReminderAtMillis(long targetDateMillis, int dayOffset) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(targetDateMillis);
        c.add(Calendar.DAY_OF_MONTH, dayOffset);

        // set 08:00:00.000 local time
        c.set(Calendar.HOUR_OF_DAY, REMIND_HOUR);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    @NonNull
    private static PendingIntent buildPendingIntent(@NonNull Context context,
                                                   @NonNull String eventId,
                                                   @Nullable String title,
                                                   @NonNull String type) {
        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.setAction(ACTION_EVENT_REMINDER);
        intent.putExtra(EXTRA_EVENT_ID, eventId);
        intent.putExtra(EXTRA_EVENT_TYPE, type);
        if (title != null) {
            intent.putExtra(EXTRA_EVENT_TITLE, title);
        }

        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;

        return PendingIntent.getBroadcast(
                context,
                stableRequestCode(eventId, type),
                intent,
                flags
        );
    }

    private static int stableRequestCode(@NonNull String eventId, @NonNull String type) {
        return (eventId + "_" + type).hashCode();
    }

    @Nullable
    private static String safeTrim(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}


