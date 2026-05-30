package com.example.ticktok.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.example.ticktok.R;
import com.example.ticktok.adapter.FocusRecordAdapter;
import com.example.ticktok.model.FocusRecord;
import com.example.ticktok.model.PomodoroSession;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.text.SimpleDateFormat;
import android.graphics.Color;

public class FocusStatisticsActivity extends AppCompatActivity {

    private TextView tvTodayPomo;
    private TextView tvTodayFocusHours;
    private TextView tvTotalPomo;
    private TextView tvTotalFocusDuration;

    private FocusRecordAdapter recordAdapter;
    private PieChart pieChartFocus;


    @NonNull
    private String lastLoadedDayKey = "";

    private final Handler dayChangeHandler = new Handler(Looper.getMainLooper());
    private final Runnable dayChangeRunnable = new Runnable() {
        @Override
        public void run() {

            lastLoadedDayKey = getTodayKey();
            loadPomodoroData();
            scheduleNextMidnightRefresh();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_focus_statistics);

        applyEdgeToEdgeInsets();

        ImageButton btnClose = findViewById(R.id.btnFocusStatsClose);
        ImageButton btnShare = findViewById(R.id.btnFocusStatsShare);

        tvTodayPomo = findViewById(R.id.tvTodayPomo);
        tvTodayFocusHours = findViewById(R.id.tvTodayFocusHours);
        tvTotalPomo = findViewById(R.id.tvTotalPomo);
        tvTotalFocusDuration = findViewById(R.id.tvTotalFocusDuration);

        RecyclerView rvFocusRecord = findViewById(R.id.rvFocusRecord);
        pieChartFocus = findViewById(R.id.pieChartFocus);

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> finish());
        }

        if (btnShare != null) {
            btnShare.setOnClickListener(v -> shareCurrentStatistics());
        }

        recordAdapter = new FocusRecordAdapter();
        if (rvFocusRecord != null) {
            rvFocusRecord.setLayoutManager(new LinearLayoutManager(this));
            rvFocusRecord.setAdapter(recordAdapter);
        }

        setupPieChart();


        lastLoadedDayKey = getTodayKey();
        loadPomodoroData();
    }

    @Override
    protected void onResume() {
        super.onResume();


        String todayKey = getTodayKey();
        if (!todayKey.equals(lastLoadedDayKey)) {
            lastLoadedDayKey = todayKey;
            loadPomodoroData();
        }

        scheduleNextMidnightRefresh();
    }

    @Override
    protected void onPause() {
        super.onPause();
        dayChangeHandler.removeCallbacks(dayChangeRunnable);
    }

    private void scheduleNextMidnightRefresh() {
        dayChangeHandler.removeCallbacks(dayChangeRunnable);

        Calendar nextMidnight = Calendar.getInstance();
        nextMidnight.add(Calendar.DAY_OF_MONTH, 1);
        nextMidnight.set(Calendar.HOUR_OF_DAY, 0);
        nextMidnight.set(Calendar.MINUTE, 0);
        nextMidnight.set(Calendar.SECOND, 0);
        nextMidnight.set(Calendar.MILLISECOND, 0);

        long delayMillis = nextMidnight.getTimeInMillis() - System.currentTimeMillis();
        if (delayMillis < 1_000L) {
            delayMillis = 1_000L;
        }
        dayChangeHandler.postDelayed(dayChangeRunnable, delayMillis);
    }

    private void setupPieChart() {
        if (pieChartFocus == null) {
            return;
        }


        pieChartFocus.setDrawEntryLabels(false);
        pieChartFocus.setUsePercentValues(false);
        pieChartFocus.setRotationEnabled(true);
        pieChartFocus.setHighlightPerTapEnabled(true);


        pieChartFocus.setDrawHoleEnabled(true);
        pieChartFocus.setHoleRadius(62f);
        pieChartFocus.setTransparentCircleRadius(66f);
        pieChartFocus.setHoleColor(Color.parseColor("#121212"));

        pieChartFocus.setCenterText(getString(R.string.focus_stats_chart_center_today));
        pieChartFocus.setCenterTextColor(Color.WHITE);
        pieChartFocus.setCenterTextSize(14f);


        Description description = pieChartFocus.getDescription();
        if (description != null) {
            description.setEnabled(false);
        }


        Legend legend = pieChartFocus.getLegend();
        if (legend != null) {
            legend.setEnabled(false);
        }

        pieChartFocus.setNoDataText("Chưa có dữ liệu để vẽ biểu đồ");
        pieChartFocus.setNoDataTextColor(Color.parseColor("#B0B0B0"));
        pieChartFocus.invalidate();
    }


    private void loadPomodoroData() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, getString(R.string.focus_stats_login_required), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String uid = user.getUid();

        FirebaseFirestore db = FirebaseFirestore.getInstance();


        db.collection("users")
                .document(uid)
                .collection("pomodoro")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(300)
                .get()
                .addOnSuccessListener(this::handlePomodoroSnapshot)
                .addOnFailureListener(e ->
                        Toast.makeText(this, getString(R.string.focus_stats_load_failed), Toast.LENGTH_SHORT).show()
                );
    }

    private void handlePomodoroSnapshot(@NonNull QuerySnapshot snapshot) {
        List<PomodoroSession> sessions = new ArrayList<>();
        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            PomodoroSession s = doc.toObject(PomodoroSession.class);
            if (s == null) {
                continue;
            }
            s.setId(doc.getId());
            sessions.add(s);
        }


        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        final long todayStartMillis = cal.getTimeInMillis();


        final long weekStartMillis = getStartOfWeekMillis();
        final long nowMillis = System.currentTimeMillis();

        long weekPomo = 0;
        long weekMinutes = 0;
        long todayPomo = 0;
        long todayMinutes = 0;
        List<PomodoroSession> todaySessions = new ArrayList<>();

        for (PomodoroSession s : sessions) {
            long durationMin = Math.max(0, s.getDuration());

            long ts = s.getTimestamp();
            boolean isInWeek = ts >= weekStartMillis && ts <= nowMillis;
            if (isInWeek) {
                weekPomo += 1;
                weekMinutes += durationMin;
            }


            if (ts >= todayStartMillis && ts <= nowMillis) {
                todayPomo += 1;
                todayMinutes += durationMin;
                todaySessions.add(s);
            }
        }


        if (tvTotalPomo != null) {
            tvTotalPomo.setText(String.valueOf(weekPomo));
        }
        if (tvTotalFocusDuration != null) {
            tvTotalFocusDuration.setText(formatMinutesToHourMinuteLabel(weekMinutes));
        }
        if (tvTodayPomo != null) {
            tvTodayPomo.setText(String.valueOf(todayPomo));
        }
        if (tvTodayFocusHours != null) {
            double hours = todayMinutes / 60.0;
            tvTodayFocusHours.setText(String.format(Locale.getDefault(), "%.1f", hours));
        }


        loadChartData(todaySessions);


        List<FocusRecord> records = new ArrayList<>();
        int maxItems = Math.min(40, todaySessions.size());
        for (int i = 0; i < maxItems; i++) {
            PomodoroSession s = todaySessions.get(i);
            records.add(mapSessionToRecord(s));
        }
        if (recordAdapter != null) {
            recordAdapter.submitList(records);
        }
    }


    private long getStartOfWeekMillis() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);

        int firstDow = Calendar.MONDAY; // Thứ 2
        int currentDow = c.get(Calendar.DAY_OF_WEEK);
        int diff = (7 + (currentDow - firstDow)) % 7;
        c.add(Calendar.DAY_OF_MONTH, -diff);
        return c.getTimeInMillis();
    }

    @NonNull
    private String getTodayKey() {
        Calendar c = Calendar.getInstance();
        return String.format(Locale.US, "%04d%02d%02d",
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH) + 1,
                c.get(Calendar.DAY_OF_MONTH));
    }


    private void loadChartData(@NonNull List<PomodoroSession> sessions) {
        if (pieChartFocus == null) {
            return;
        }

        if (sessions.isEmpty()) {
            pieChartFocus.clear();
            pieChartFocus.invalidate();
            return;
        }


        Map<String, Long> durationByTask = new HashMap<>();
        for (PomodoroSession s : sessions) {
            String name = s.getTaskName();
            if (name == null) {
                name = "(Không rõ)";
            }
            name = name.trim();
            if (name.isEmpty()) {
                name = "(Không rõ)";
            }

            long durationMin = Math.max(0L, s.getDuration());
            Long current = durationByTask.get(name);
            durationByTask.put(name, (current == null ? 0L : current) + durationMin);
        }


        ArrayList<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Long> e : durationByTask.entrySet()) {
            if (e.getValue() == null) {
                continue;
            }
            float minutes = Math.max(0f, e.getValue());
            if (minutes <= 0f) {
                continue;
            }
            entries.add(new PieEntry(minutes, e.getKey()));
        }

        if (entries.isEmpty()) {
            pieChartFocus.clear();
            pieChartFocus.invalidate();
            return;
        }


        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setSliceSpace(2f);
        dataSet.setSelectionShift(6f);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(12f);

        ArrayList<Integer> colors = new ArrayList<>();
        colors.add(Color.parseColor("#FF8C42")); // cam
        colors.add(Color.parseColor("#FFD166")); // vàng
        colors.add(Color.parseColor("#EF476F")); // đỏ hồng
        colors.add(Color.parseColor("#06D6A0")); // xanh lá
        colors.add(Color.parseColor("#118AB2")); // xanh dương
        colors.add(Color.parseColor("#9B5DE5")); // tím
        dataSet.setColors(colors);


        PieData data = new PieData(dataSet);
        data.setValueFormatter(new ValueFormatter() {
            @Override
            public String getPieLabel(float value, PieEntry pieEntry) {
                return String.format(Locale.getDefault(), "%.0fp", value);
            }
        });
        data.setValueTextColor(Color.WHITE);
        data.setValueTextSize(12f);

        pieChartFocus.setData(data);
        pieChartFocus.animateY(1000);
        pieChartFocus.invalidate();
    }

    @NonNull
    private FocusRecord mapSessionToRecord(@NonNull PomodoroSession session) {
        String title = session.getTaskName();
        if (TextUtils.isEmpty(title)) {
            title = "Pomodoro";
        }

        String subtitle = formatVietnameseRelativeDateTime(session.getTimestamp());
        String durationLabel = String.format(Locale.getDefault(), "%dp", Math.max(0, session.getDuration()));
        String tag = "TẬP TRUNG";

        return new FocusRecord(title, subtitle, durationLabel, tag);
    }


    @NonNull
    private String formatMinutesToHourMinuteLabel(long minutes) {
        long safe = Math.max(0, minutes);
        long h = safe / 60;
        long m = safe % 60;
        return String.format(Locale.getDefault(), "%dh %02dm", h, m);
    }


    @NonNull
    private String formatVietnameseRelativeDateTime(long timestampMillis) {
        Calendar todayStart = Calendar.getInstance();
        todayStart.set(Calendar.HOUR_OF_DAY, 0);
        todayStart.set(Calendar.MINUTE, 0);
        todayStart.set(Calendar.SECOND, 0);
        todayStart.set(Calendar.MILLISECOND, 0);

        Calendar yesterdayStart = (Calendar) todayStart.clone();
        yesterdayStart.add(Calendar.DAY_OF_MONTH, -1);

        String dayLabel;
        if (timestampMillis >= todayStart.getTimeInMillis()) {
            dayLabel = "Hôm nay";
        } else if (timestampMillis >= yesterdayStart.getTimeInMillis()) {
            dayLabel = "Hôm qua";
        } else {
            SimpleDateFormat df = new SimpleDateFormat("dd/MM", Locale.getDefault());
            dayLabel = df.format(timestampMillis);
        }

        SimpleDateFormat tf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String time = tf.format(timestampMillis);
        return dayLabel + " • " + time;
    }

    private void shareCurrentStatistics() {
        String todayPomo = tvTodayPomo != null ? tvTodayPomo.getText().toString() : "0";
        String todayHours = tvTodayFocusHours != null ? tvTodayFocusHours.getText().toString() : "0.0";
        String totalPomo = tvTotalPomo != null ? tvTotalPomo.getText().toString() : "0";
        String totalDuration = tvTotalFocusDuration != null ? tvTotalFocusDuration.getText().toString() : "0h 00m";

        String shareText = "Thống kê Pomodoro\n" +
                "Hôm nay: " + todayPomo + " phiên • " + todayHours + "h\n" +
                "Tuần này: " + totalPomo + " phiên • " + totalDuration;

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);

        try {
            startActivity(Intent.createChooser(shareIntent, getString(R.string.focus_stats_share_chooser_title)));
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.focus_stats_share_unavailable), Toast.LENGTH_SHORT).show();
        }
    }


    private void applyEdgeToEdgeInsets() {
        final android.view.View root = findViewById(R.id.layoutFocusStatisticsRoot);
        if (root == null) {
            return;
        }

        final int initialLeft = root.getPaddingLeft();
        final int initialTop = root.getPaddingTop();
        final int initialRight = root.getPaddingRight();
        final int initialBottom = root.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            final int systemBars = WindowInsetsCompat.Type.systemBars();
            final androidx.core.graphics.Insets bars = insets.getInsets(systemBars);
            view.setPadding(
                    initialLeft,
                    initialTop + bars.top,
                    initialRight,
                    initialBottom + bars.bottom
            );
            return insets;
        });

        ViewCompat.requestApplyInsets(root);
    }
}







