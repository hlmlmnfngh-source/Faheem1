package com.example.faheem;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class VerificationActivity extends AppCompatActivity {

    private EditText code1, code2, code3, code4, code5, code6;
    private Button confirmButton;
    private TextView resendTextView, timerTextView;
    private String userEmail;

    private static final long CODE_VALID_DURATION_MS = 10 * 60 * 1000; // 10 دقائق

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verification);

        userEmail = getIntent().getStringExtra("email");

        code1 = findViewById(R.id.code1);
        code2 = findViewById(R.id.code2);
        code3 = findViewById(R.id.code3);
        code4 = findViewById(R.id.code4);
        code5 = findViewById(R.id.code5);
        code6 = findViewById(R.id.code6);
        confirmButton = findViewById(R.id.confirmButton);
        resendTextView = findViewById(R.id.resendTextView);
        timerTextView = findViewById(R.id.timerTextView);

        confirmButton.setEnabled(false);

        setupOtpLogic();

        confirmButton.setOnClickListener(v -> verifyCode());

        resendTextView.setOnClickListener(v -> {
            if (resendTextView.isEnabled()) {
                resendNewCode();
            }
        });

        startResendTimer();
    }

    private void setupOtpLogic() {
        EditText[] boxes = {code1, code2, code3, code4, code5, code6};

        for (int i = 0; i < boxes.length; i++) {
            final int index = i;
            boxes[i].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.length() == 1 && index < 5) {
                        boxes[index + 1].requestFocus();
                    }
                    checkConfirmButtonState();
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });

            // دعم زر الحذف للرجوع للخلف
            boxes[i].setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (boxes[index].getText().length() == 0 && index > 0) {
                        boxes[index - 1].requestFocus();
                        boxes[index - 1].setText("");
                        return true;
                    }
                }
                return false;
            });
        }
    }

    private void checkConfirmButtonState() {
        String enteredCode = getOtpValue();
        boolean isFull = enteredCode.length() == 6;
        confirmButton.setEnabled(isFull);
        
        int color = isFull ? R.color.brand_primary_green : R.color.consent_button_disabled;
        confirmButton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, color)));
    }

    private String getOtpValue() {
        return code1.getText().toString() + code2.getText().toString() + 
               code3.getText().toString() + code4.getText().toString() + 
               code5.getText().toString() + code6.getText().toString();
    }

    private void verifyCode() {
        String enteredCode = getOtpValue();
        AppDatabase db = AppDatabase.getInstance(this);

        AppDatabase.databaseExecutor.execute(() -> {
            User user = db.userDao().getUserByEmail(userEmail);

            runOnUiThread(() -> {
                if (user == null) {
                    Toast.makeText(this, getString(R.string.toast_user_missing), Toast.LENGTH_SHORT).show();
                    return;
                }

                long elapsed = System.currentTimeMillis() - user.getCodeGeneratedAt();

                if (elapsed > CODE_VALID_DURATION_MS) {
                    Toast.makeText(this, getString(R.string.toast_code_expired), Toast.LENGTH_LONG).show();
                } else if (!enteredCode.equals(user.getVerificationCode())) {
                    Toast.makeText(this, getString(R.string.toast_code_wrong), Toast.LENGTH_SHORT).show();
                } else {
                    // تم التحقق بنجاح
                    user.setVerified(true);
                    user.setVerificationCode(null);
                    AppDatabase.databaseExecutor.execute(() -> db.userDao().updateUser(user));
                    new SessionManager(VerificationActivity.this).startSession(user.getId());
                    Intent intent = new Intent(VerificationActivity.this, HomeActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }
            });
        });
    }

    private void resendNewCode() {
        String newCode = EmailSender.generateCode();
        AppDatabase db = AppDatabase.getInstance(this);

        AppDatabase.databaseExecutor.execute(() -> {
            User user = db.userDao().getUserByEmail(userEmail);
            if (user != null) {
                user.setVerificationCode(newCode);
                user.setCodeGeneratedAt(System.currentTimeMillis());
                db.userDao().updateUser(user);

                EmailSender sender = new EmailSender();
                sender.sendVerificationCode(userEmail, newCode, new EmailSender.SendCallback() {
                    @Override
                    public void onSuccess() {
                        runOnUiThread(() -> {
                            Toast.makeText(VerificationActivity.this, getString(R.string.toast_code_resent), Toast.LENGTH_SHORT).show();
                            startResendTimer();
                        });
                    }

                    @Override
                    public void onFailure(String error) {
                        runOnUiThread(() -> Toast.makeText(VerificationActivity.this, getString(R.string.toast_resend_failed, error), Toast.LENGTH_LONG).show());
                    }
                });
            }
        });
    }

    private void startResendTimer() {
        resendTextView.setEnabled(false);
        resendTextView.setAlpha(0.5f);
        new CountDownTimer(30000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int seconds = (int) (millisUntilFinished / 1000);
                timerTextView.setText(String.format("00:%02d", seconds));
            }

            @Override
            public void onFinish() {
                timerTextView.setText("");
                resendTextView.setEnabled(true);
                resendTextView.setAlpha(1.0f);
            }
        }.start();
    }
}
