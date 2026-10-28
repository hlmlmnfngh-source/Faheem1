package com.example.faheem;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private EditText emailEditText, passwordEditText;
    private Button loginButton, googleButton, facebookButton;
    private TextView createAccountTextView, forgotPasswordTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        googleButton = findViewById(R.id.googleButton);
        facebookButton = findViewById(R.id.facebookButton);
        createAccountTextView = findViewById(R.id.createAccountTextView);
        forgotPasswordTextView = findViewById(R.id.forgotPasswordTextView);

        loginButton.setOnClickListener(v -> attemptLogin());

        createAccountTextView.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, CreateAccountActivity.class)));

        googleButton.setOnClickListener(v -> startGoogleSignIn());

        facebookButton.setOnClickListener(v -> {
            // TODO: ربط Facebook Login — مرحلة لاحقة
        });
    }

    private void attemptLogin() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, getString(R.string.toast_login_fill), Toast.LENGTH_SHORT).show();
            return;
        }

        AppDatabase db = AppDatabase.getInstance(this);

        AppDatabase.databaseExecutor.execute(() -> {
            User user = db.userDao().getUserByEmail(email);

            runOnUiThread(() -> {
                boolean passwordMatches = user != null && ((user.getPasswordHash() != null && PasswordHasher.verify(password, user.getPasswordHash())) || (user.getPasswordHash() == null && password.equals(user.getPassword())));
                if (!passwordMatches) {
                    Toast.makeText(this, getString(R.string.toast_login_wrong), Toast.LENGTH_SHORT).show();
                } else {
                    if (!user.isVerified()) {
                        Intent verificationIntent = new Intent(LoginActivity.this, VerificationActivity.class);
                        verificationIntent.putExtra("email", user.getEmail());
                        startActivity(verificationIntent);
                        return;
                    }
                    if (user.getPasswordHash() == null) {
                        user.setPasswordHash(PasswordHasher.hash(password));
                        user.setPassword(null);
                        AppDatabase.databaseExecutor.execute(() -> db.userDao().updateUser(user));
                    }
                    new SessionManager(LoginActivity.this).startSession(user.getId());
                    Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }
            });
        });
    }

    private void startGoogleSignIn() {
        if (!PrivacyPreferences.hasRemoteProcessingConsent(this)) {
            startActivity(new Intent(this, PrivacyConsentActivity.class));
            return;
        }
        new GoogleSignInHelper().signIn(this, new GoogleSignInHelper.Callback() {
            @Override public void onSuccess(com.google.firebase.auth.FirebaseUser firebaseUser) {
                if (firebaseUser == null || firebaseUser.getEmail() == null) {
                    Toast.makeText(LoginActivity.this, R.string.google_sign_in_failed, Toast.LENGTH_LONG).show();
                    return;
                }
                AppDatabase.databaseExecutor.execute(() -> {
                    User user = AppDatabase.getInstance(LoginActivity.this).userDao()
                            .getUserByEmail(firebaseUser.getEmail());
                    runOnUiThread(() -> {
                        if (user == null) {
                            Toast.makeText(LoginActivity.this, R.string.google_account_not_registered, Toast.LENGTH_LONG).show();
                            return;
                        }
                        if (!user.isVerified()) {
                            user.setVerified(true);
                            AppDatabase.databaseExecutor.execute(() -> AppDatabase.getInstance(LoginActivity.this).userDao().updateUser(user));
                        }
                        new SessionManager(LoginActivity.this).startSession(user.getId());
                        Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    });
                });
            }
            @Override public void onError(int messageRes) {
                Toast.makeText(LoginActivity.this, messageRes, Toast.LENGTH_LONG).show();
            }
        });
    }
}
