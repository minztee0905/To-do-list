package com.example.ticktok.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ticktok.R;
import com.example.ticktok.util.AuthAccountMapper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserProfileChangeRequest;

public class RegisterActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;
    private TextInputEditText etRegisterUsername;
    private TextInputEditText etRegisterPassword;
    private TextInputEditText etRegisterConfirmPassword;
    private MaterialButton btnRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        firebaseAuth = FirebaseAuth.getInstance();

        etRegisterUsername = findViewById(R.id.etRegisterUsername);
        etRegisterPassword = findViewById(R.id.etRegisterPassword);
        etRegisterConfirmPassword = findViewById(R.id.etRegisterConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        TextView tvGoLogin = findViewById(R.id.tvGoLogin);

        if (btnRegister != null) {
            btnRegister.setOnClickListener(v -> registerUser());
        }

        if (tvGoLogin != null) {
            tvGoLogin.setOnClickListener(v -> finish());
        }
    }

    private void registerUser() {
        String accountInput = etRegisterUsername != null && etRegisterUsername.getText() != null
                ? etRegisterUsername.getText().toString().trim()
                : "";
        String password = etRegisterPassword != null && etRegisterPassword.getText() != null
                ? etRegisterPassword.getText().toString().trim()
                : "";
        String confirmPassword = etRegisterConfirmPassword != null && etRegisterConfirmPassword.getText() != null
                ? etRegisterConfirmPassword.getText().toString().trim()
                : "";

        if (TextUtils.isEmpty(accountInput) || TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword)) {
            Toast.makeText(this, getString(R.string.auth_error_empty_fields), Toast.LENGTH_SHORT).show();
            return;
        }

        String authEmail = AuthAccountMapper.toAuthEmail(accountInput);
        if (TextUtils.isEmpty(authEmail)) {
            Toast.makeText(this, getString(R.string.auth_error_account_invalid), Toast.LENGTH_SHORT).show();
            return;
        }

        String displayName = AuthAccountMapper.normalizeDisplayName(accountInput);

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, getString(R.string.auth_error_password_mismatch), Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, getString(R.string.auth_error_password_too_short), Toast.LENGTH_SHORT).show();
            return;
        }

        setRegisterLoading(true);
        firebaseAuth.createUserWithEmailAndPassword(authEmail, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        if (firebaseAuth.getCurrentUser() != null && !TextUtils.isEmpty(displayName)) {
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(displayName)
                                    .build();
                            firebaseAuth.getCurrentUser().updateProfile(profileUpdates)
                                    .addOnCompleteListener(profileTask -> {
                                        setRegisterLoading(false);
                                        Toast.makeText(this, getString(R.string.register_success), Toast.LENGTH_SHORT).show();
                                        Intent intent = new Intent(this, MainActivity.class);
                                        startActivity(intent);
                                        finishAffinity();
                                    });
                            return;
                        }

                        setRegisterLoading(false);
                        Toast.makeText(this, getString(R.string.register_success), Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(this, MainActivity.class);
                        startActivity(intent);
                        finishAffinity();
                    } else {
                        setRegisterLoading(false);
                        String fallbackMessage = getString(R.string.auth_error_register_failed);
                        String errorMessage = task.getException() != null && task.getException().getMessage() != null
                                ? task.getException().getMessage()
                                : fallbackMessage;
                        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setRegisterLoading(boolean isLoading) {
        if (btnRegister == null) {
            return;
        }
        btnRegister.setEnabled(!isLoading);
        btnRegister.setText(isLoading ? R.string.auth_loading : R.string.register_button);
    }
}



