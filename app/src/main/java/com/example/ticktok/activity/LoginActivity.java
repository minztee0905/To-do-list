package com.example.ticktok.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ticktok.R;
import com.example.ticktok.util.AuthAccountMapper;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;
    private GoogleSignInClient googleSignInClient;
    private boolean isGoogleSignInConfigured;
    private TextInputEditText etEmail;
    private TextInputEditText etPassword;
    private MaterialButton btnLogin;
    private MaterialButton btnGoogleSignIn;

    private final ActivityResultLauncher<Intent> googleSignInLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                Intent data = result.getData();
                if (data == null) {
                    setAuthLoading(false);
                    Toast.makeText(this, getString(R.string.auth_error_google_sign_in_failed), Toast.LENGTH_SHORT).show();
                    return;
                }

                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                try {
                    GoogleSignInAccount account = task.getResult(ApiException.class);
                    if (account == null || account.getIdToken() == null) {
                        setAuthLoading(false);
                        Toast.makeText(this, getString(R.string.auth_error_google_token_missing), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    firebaseAuthWithGoogle(account.getIdToken());
                } catch (ApiException exception) {
                    setAuthLoading(false);
                    Toast.makeText(this, getString(R.string.auth_error_google_sign_in_failed), Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        firebaseAuth = FirebaseAuth.getInstance();
        if (firebaseAuth.getCurrentUser() != null) {
            goToMainAndFinish();
            return;
        }

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
        TextView tvSignUpNow = findViewById(R.id.tvSignUpNow);

        String webClientId = getString(R.string.default_web_client_id).trim();
        isGoogleSignInConfigured = !TextUtils.isEmpty(webClientId)
                && !"YOUR_WEB_CLIENT_ID".equals(webClientId);
        if (isGoogleSignInConfigured) {
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(webClientId)
                    .requestEmail()
                    .build();
            googleSignInClient = GoogleSignIn.getClient(this, gso);
        }

        if (btnLogin != null) {
            btnLogin.setOnClickListener(v -> {
                hideKeyboard(v);
                clearInputFocus();
                loginUser();
            });
        }

        if (tvSignUpNow != null) {
            tvSignUpNow.setOnClickListener(v -> {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            });
        }

        if (btnGoogleSignIn != null) {
            btnGoogleSignIn.setOnClickListener(v -> startGoogleSignIn());
            if (!isGoogleSignInConfigured) {
                btnGoogleSignIn.setAlpha(0.6f);
            }
        }

        setupKeyboardDismissBehavior();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            View currentFocus = getCurrentFocus();
            if (currentFocus instanceof TextInputEditText) {
                int[] location = new int[2];
                currentFocus.getLocationOnScreen(location);
                float x = ev.getRawX();
                float y = ev.getRawY();

                boolean isOutsideFocusedInput = x < location[0]
                        || x > location[0] + currentFocus.getWidth()
                        || y < location[1]
                        || y > location[1] + currentFocus.getHeight();

                if (isOutsideFocusedInput) {
                    hideKeyboard(currentFocus);
                    clearInputFocus();
                }
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    private void loginUser() {
        String accountInput = etEmail != null && etEmail.getText() != null
                ? etEmail.getText().toString().trim()
                : "";
        String password = etPassword != null && etPassword.getText() != null
                ? etPassword.getText().toString().trim()
                : "";

        if (TextUtils.isEmpty(accountInput) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, getString(R.string.auth_error_empty_fields), Toast.LENGTH_SHORT).show();
            return;
        }

        String authEmail = AuthAccountMapper.toAuthEmail(accountInput);
        if (TextUtils.isEmpty(authEmail)) {
            Toast.makeText(this, getString(R.string.auth_error_account_invalid), Toast.LENGTH_SHORT).show();
            return;
        }

        setAuthLoading(true);
        firebaseAuth.signInWithEmailAndPassword(authEmail, password)
                .addOnCompleteListener(this, task -> {
                    setAuthLoading(false);
                    if (task.isSuccessful()) {
                        goToMainAndFinish();
                    } else {
                        String fallbackMessage = getString(R.string.auth_error_login_failed);
                        String errorMessage = task.getException() != null && task.getException().getMessage() != null
                                ? task.getException().getMessage()
                                : fallbackMessage;
                        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void startGoogleSignIn() {
        if (!isGoogleSignInConfigured) {
            Toast.makeText(this, getString(R.string.auth_error_google_not_configured), Toast.LENGTH_SHORT).show();
            return;
        }
        if (googleSignInClient == null) {
            Toast.makeText(this, getString(R.string.auth_error_google_sign_in_failed), Toast.LENGTH_SHORT).show();
            return;
        }
        setAuthLoading(true);
        googleSignInLauncher.launch(googleSignInClient.getSignInIntent());
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    setAuthLoading(false);
                    if (task.isSuccessful()) {
                        goToMainAndFinish();
                    } else {
                        String fallbackMessage = getString(R.string.auth_error_google_sign_in_failed);
                        String errorMessage = task.getException() != null && task.getException().getMessage() != null
                                ? task.getException().getMessage()
                                : fallbackMessage;
                        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setAuthLoading(boolean isLoading) {
        if (btnLogin != null) {
            btnLogin.setEnabled(!isLoading);
            btnLogin.setText(isLoading ? R.string.auth_loading : R.string.login_button);
        }
        if (btnGoogleSignIn != null) {
            btnGoogleSignIn.setEnabled(!isLoading);
        }
    }

    private void goToMainAndFinish() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void setupKeyboardDismissBehavior() {
        if (etEmail != null) {
            etEmail.setOnFocusChangeListener((v, hasFocus) -> {
                if (!hasFocus) {
                    hideKeyboard(v);
                }
            });
        }
        if (etPassword != null) {
            etPassword.setOnFocusChangeListener((v, hasFocus) -> {
                if (!hasFocus) {
                    hideKeyboard(v);
                }
            });
        }
    }

    private void clearInputFocus() {
        if (etEmail != null) {
            etEmail.clearFocus();
        }
        if (etPassword != null) {
            etPassword.clearFocus();
        }
    }

    private void hideKeyboard(View anchorView) {
        if (anchorView == null) {
            return;
        }
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(anchorView.getWindowToken(), 0);
        }
    }
}

