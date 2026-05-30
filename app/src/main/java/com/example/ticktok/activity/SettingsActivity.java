package com.example.ticktok.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.ticktok.R;
import com.example.ticktok.TickTokApplication;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SettingsActivity extends AppCompatActivity {

    private static final String PREFS_SETTINGS = TickTokApplication.PREFS_SETTINGS;
    private static final String KEY_SOUND_NOTIFICATIONS = "sound_notifications_enabled";
    private static final String KEY_THEME_MODE = TickTokApplication.KEY_THEME_MODE;

    private ImageButton btnSettingsBack;
    private ImageView ivSettingsAvatar;
    private TextView tvSettingsUserName;
    private View rowTheme;
    private View rowSound;
    private View rowPomodoro;
    private View rowAppInfo;
    private android.widget.Button btnLogout;
    private View rowPassword;
    private View dividerPassword;

    @Nullable
    private SwitchCompat switchSoundNotifications;

    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);

        applyEdgeToEdgeInsets();

        firebaseAuth = FirebaseAuth.getInstance();

        // Map views
        dividerPassword = findViewById(R.id.dividerPassword);
        rowPassword = findViewById(R.id.rowSettingPassword);
        btnSettingsBack = findViewById(R.id.btnSettingsBack);
        ivSettingsAvatar = findViewById(R.id.ivSettingsAvatar);
        tvSettingsUserName = findViewById(R.id.tvSettingsUserName);
        btnLogout = findViewById(R.id.btnLogout);

        rowTheme = findViewById(R.id.rowSettingTheme);
        rowSound = findViewById(R.id.rowSettingSound);
        rowAppInfo = findViewById(R.id.rowSettingAppInfo);
        switchSoundNotifications = findViewById(R.id.switchSoundNotifications);
        if (rowPassword != null) rowPassword.setOnClickListener(v -> showChangePasswordDialog());

        if (btnSettingsBack != null) btnSettingsBack.setOnClickListener(v -> finish());

        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            boolean isPasswordAccount = false;


            for (com.google.firebase.auth.UserInfo userInfo : user.getProviderData()) {
                if (userInfo.getProviderId().equals("password")) {
                    isPasswordAccount = true;
                    break;
                }
            }

            if (!isPasswordAccount) {
                if (rowPassword != null) rowPassword.setVisibility(View.GONE);
                if (dividerPassword != null) dividerPassword.setVisibility(View.GONE);
            }
        }
        if (tvSettingsUserName != null) tvSettingsUserName.setText(resolveAccountText(user));

        setupSoundNotificationSwitch();

        if (rowSound != null && switchSoundNotifications != null) {
            rowSound.setOnClickListener(v -> switchSoundNotifications.toggle());
        }

        if (rowTheme != null) rowTheme.setOnClickListener(v -> showThemeDialog());
        if (rowPomodoro != null) rowPomodoro.setOnClickListener(v -> showComingSoon());
        if (rowAppInfo != null) rowAppInfo.setOnClickListener(v -> showAppInfoDialog());

        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> confirmLogout());
        }
    }

    private void showChangePasswordDialog() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) return;

        final android.widget.EditText input = new android.widget.EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setHint("Nhập mật khẩu mới (từ 6 ký tự)");
        input.setTextColor(android.graphics.Color.WHITE);
        input.setHintTextColor(android.graphics.Color.GRAY);

        android.widget.FrameLayout container = new android.widget.FrameLayout(this);
        android.widget.FrameLayout.LayoutParams params = new android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(50, 20, 50, 0);
        input.setLayoutParams(params);
        container.addView(input);

        new AlertDialog.Builder(this)
                .setTitle("Đổi mật khẩu")
                .setView(container)
                .setPositiveButton("Cập nhật", (dialog, which) -> {
                    String newPassword = input.getText().toString().trim();

                    if (newPassword.length() < 6) {
                        Toast.makeText(this, "Mật khẩu quá ngắn, phải từ 6 ký tự trở lên!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    user.updatePassword(newPassword)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Toast.makeText(this, "Đổi mật khẩu thành công.", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(this, "Lỗi: Vui lòng đăng xuất và đăng nhập lại trước khi đổi mật khẩu!", Toast.LENGTH_LONG).show();
                                }
                            });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showAppInfoDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Thông tin ứng dụng")
                .setMessage("Ứng dụng: TickTok\nPhiên bản: 1.0.0\nTác giả: Trần Minh Triết\n\nChúc bạn có những giờ phút tập trung hiệu quả và hoàn thành tốt dự án 100 days 1000 hours!")
                .setPositiveButton("Đóng", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void setupSoundNotificationSwitch() {
        if (switchSoundNotifications == null) {
            return;
        }

        SharedPreferences prefs = getSharedPreferences(PREFS_SETTINGS, MODE_PRIVATE);
        boolean enabled = prefs.getBoolean(KEY_SOUND_NOTIFICATIONS, true);
        switchSoundNotifications.setChecked(enabled);

        switchSoundNotifications.setOnCheckedChangeListener((buttonView, isChecked) ->
                prefs.edit().putBoolean(KEY_SOUND_NOTIFICATIONS, isChecked).apply()
        );
    }

    private void showComingSoon() {
        Toast.makeText(this, getString(R.string.settings_feature_coming_soon), Toast.LENGTH_SHORT).show();
    }

    private void showThemeDialog() {
        final SharedPreferences prefs = getSharedPreferences(PREFS_SETTINGS, MODE_PRIVATE);
        final int currentTheme = prefs.getInt(KEY_THEME_MODE, TickTokApplication.THEME_SYSTEM);

        final String[] items = new String[]{
                getString(R.string.settings_theme_system),
                getString(R.string.settings_theme_light),
                getString(R.string.settings_theme_dark)
        };

        new AlertDialog.Builder(this)
                .setTitle(R.string.settings_theme)
                .setSingleChoiceItems(items, currentTheme, (dialog, which) -> {
                    prefs.edit().putInt(KEY_THEME_MODE, which).apply();
                    AppCompatDelegate.setDefaultNightMode(TickTokApplication.mapToNightMode(which));
                    dialog.dismiss();
                    recreate();
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.settings_logout_confirm_title)
                .setMessage(R.string.settings_logout_confirm_message)
                .setNegativeButton(R.string.action_no, null)
                .setPositiveButton(R.string.action_yes, (dialog, which) -> performLogout())
                .show();
    }

    private void performLogout() {
        firebaseAuth.signOut();
        navigateToLogin();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finishAffinity();
    }

    private String resolveAccountText(FirebaseUser user) {
        if (user == null) {
            return getString(R.string.menu_user_default_name);
        }

        if (user.getEmail() != null && !user.getEmail().trim().isEmpty()) {
            return user.getEmail().trim();
        }

        if (user.getDisplayName() != null && !user.getDisplayName().trim().isEmpty()) {
            return user.getDisplayName().trim();
        }


        return getString(R.string.menu_user_default_name);
    }


    private void applyEdgeToEdgeInsets() {
        final android.view.View root = findViewById(R.id.layoutSettingsRoot);
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


