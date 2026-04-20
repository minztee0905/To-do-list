package com.example.ticktok.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ticktok.R;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);

        TextView tvSettingsUserName = findViewById(R.id.tvSettingsUserName);
        MaterialButton btnLogout = findViewById(R.id.btnLogout);
        ImageButton btnSettingsBack = findViewById(R.id.btnSettingsBack);

        if (btnSettingsBack != null) {
            btnSettingsBack.setOnClickListener(v -> finish());
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (tvSettingsUserName != null) {
            tvSettingsUserName.setText(resolveDisplayName(user));
        }

        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(
                        SettingsActivity.this,
                        new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                .requestIdToken(getString(R.string.default_web_client_id))
                                .requestEmail()
                                .build()
                );

                FirebaseAuth.getInstance().signOut();
                googleSignInClient.signOut().addOnCompleteListener(task -> navigateToLogin());
            });
        }
    }

    private void navigateToLogin() {
        Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private String resolveDisplayName(FirebaseUser user) {
        if (user == null) {
            return getString(R.string.menu_user_default_name);
        }

        if (user.getDisplayName() != null && !user.getDisplayName().trim().isEmpty()) {
            return user.getDisplayName().trim();
        }

        if (user.getEmail() != null && !user.getEmail().trim().isEmpty()) {
            return user.getEmail().trim();
        }

        return getString(R.string.menu_user_default_name);
    }
}


