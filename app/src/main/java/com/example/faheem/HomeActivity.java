package com.example.faheem;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.ContextCompat;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.io.File;

public class HomeActivity extends AppCompatActivity {

    private static final int RECORD_AUDIO_PERMISSION_CODE = 100;

    // الفاصل الزمني بين تحليل وآخر للخطة المجانية (غيّر الرقم هون لو بدك مدة ثانية)
    private static final long FREE_COOLDOWN_MINUTES = 120;
    private static final long FREE_COOLDOWN_MS = FREE_COOLDOWN_MINUTES * 60 * 1000L;
    private static final String LIMITS_PREFS = "faheem_limits";

    private ImageView recordButton, settingsIcon;
    private LinearLayout navProfile, navHistory, navUpgrade;

    private AudioRecorderHelper audioRecorderHelper;
    private BillingManager billingManager;
    private boolean isRecording = false;
    private boolean isProcessingRecordRequest = false;
    private volatile boolean isAnalyzing = false; // تحليل شغّال الآن
    private int currentUserId = -1;
    private AudioRecorderHelper.RecordingCallback recordingCallback;
    private TextView trialCountTextView, homeGreeting;
    private LinearLayout recentAnalysesContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        recordButton = findViewById(R.id.recordButton);
        settingsIcon = findViewById(R.id.settingsIcon);
        navProfile = findViewById(R.id.navProfile);
        navHistory = findViewById(R.id.navHistory);
        navUpgrade = findViewById(R.id.navUpgrade);
        trialCountTextView = findViewById(R.id.trialCountTextView);
        homeGreeting = findViewById(R.id.homeGreeting);
        recentAnalysesContainer = findViewById(R.id.recentAnalysesContainer);

        audioRecorderHelper = new AudioRecorderHelper();
        billingManager = new BillingManager(this, null);
        currentUserId = new SessionManager(this).getUserId();
        if (currentUserId <= 0) {
            openLogin();
            return;
        }

        recordButton.setOnClickListener(v -> {
            if (!PrivacyPreferences.hasRemoteProcessingConsent(this)) {
                startActivity(new Intent(this, PrivacyConsentActivity.class));
                return;
            }
            if (isProcessingRecordRequest) return; // تجاهل أي ضغطة إضافية أثناء المعالجة
            if (isAnalyzing) {
                Toast.makeText(this, getString(R.string.toast_still_analyzing), Toast.LENGTH_SHORT).show();
                return;
            }

            if (!isRecording) {
                isProcessingRecordRequest = true;
                checkPermissionAndStartRecording();
            } else {
                stopRecording();
            }
        });

        // زر "اسأل فهيم" - يمكننا ربطه بأيقونة الدردشة إذا وجدت، أو إضافة مستمع لأحد عناصر القائمة
        // مؤقتاً سأجعله يفتح عند الضغط الطويل على زر التسجيل كاختصار
        recordButton.setOnLongClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, FaheemChatActivity.class));
            return true;
        });

        settingsIcon.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, SettingsActivity.class)));

        navProfile.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, ProfileActivity.class)));

        navUpgrade.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, PaymentActivity.class)));

        navHistory.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, NotificationsHistoryActivity.class)));
    }


    // ===================== بيانات الرئيسية =====================
    @Override
    protected void onResume() {
        super.onResume();
        billingManager.connect();
        if (currentUserId > 0) refreshHomeData();
    }

    private void refreshHomeData() {
        AppDatabase.databaseExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            User user = db.userDao().getUserById(currentUserId);
            if (user == null) return;

            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
            int used = today.equals(user.getLastAnalysisDate()) ? user.getDailyAnalysesUsed() : 0;
            boolean isFree = "free".equals(user.getPlanType());
            List<AnalysisResult> recent = db.analysisResultDao().getRecentByOwner(currentUserId, 5);

            runOnUiThread(() -> {
                homeGreeting.setText(getString(R.string.home_greeting_fmt, user.getFirstName()));
                if (isFree) {
                    int left = Math.max(0, 3 - used);
                    long wait = cooldownRemainingMs();
                    if (left > 0 && wait > 0) {
                        trialCountTextView.setText(getString(R.string.trials_left_wait_fmt, left, formatRemaining(wait)));
                    } else {
                        trialCountTextView.setText(getString(R.string.trials_left_fmt, left));
                    }
                } else {
                    trialCountTextView.setText(R.string.unlimited_analyses);
                }
                renderRecent(recent);
            });
        });
    }

    private void renderRecent(List<AnalysisResult> list) {
        recentAnalysesContainer.removeAllViews();
        if (list.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(R.string.no_analyses_yet);
            empty.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            empty.setPadding(0, dp(12), 0, 0);
            recentAnalysesContainer.addView(empty);
            return;
        }
        for (AnalysisResult r : list) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.HORIZONTAL);
            card.setGravity(android.view.Gravity.CENTER_VERTICAL);
            card.setBackgroundResource(R.drawable.card_bg);
            card.setPadding(dp(12), dp(12), dp(12), dp(12));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.topMargin = dp(10);
            card.setLayoutParams(lp);

            ImageView icon = new ImageView(this);
            icon.setBackgroundResource(R.drawable.bg_icon_circle);
            icon.setPadding(dp(8), dp(8), dp(8), dp(8));
            icon.setImageResource(R.drawable.ic_paw);
            icon.setLayoutParams(new LinearLayout.LayoutParams(dp(40), dp(40)));

            LinearLayout texts = new LinearLayout(this);
            texts.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            tp.setMarginStart(dp(10));
            texts.setLayoutParams(tp);

            TextView date = new TextView(this);
            date.setText(formatWhen(r.getTimestamp()));
            date.setTextSize(12);
            date.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));

            TextView mood = new TextView(this);
            mood.setText((r.isHealthAlert() ? "⚠️ " : "") + r.getResultText());
            mood.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            mood.setTypeface(mood.getTypeface(), android.graphics.Typeface.BOLD);

            texts.addView(date);
            texts.addView(mood);
            card.addView(icon);
            card.addView(texts);
            recentAnalysesContainer.addView(card);
        }
    }

    private String formatWhen(long ts) {
        Locale ar = Locale.getDefault();
        String time = new SimpleDateFormat("h:mm a", ar).format(new Date(ts));
        String day = new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date(ts));
        String today = new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date());
        String yesterday = new SimpleDateFormat("yyyyMMdd", Locale.US)
                .format(new Date(System.currentTimeMillis() - 24L * 60 * 60 * 1000));
        if (day.equals(today)) return getString(R.string.today_fmt, time);
        if (day.equals(yesterday)) return getString(R.string.yesterday_fmt, time);
        return getString(R.string.date_time_fmt, new SimpleDateFormat("d MMM", ar).format(new Date(ts)), time);
    }

    // ===================== الفاصل الزمني للخطة المجانية =====================
    private String lastAnalysisKey() { return "last_analysis_ms_" + currentUserId; }

    private long getLastAnalysisMs() {
        return getSharedPreferences(LIMITS_PREFS, MODE_PRIVATE).getLong(lastAnalysisKey(), 0L);
    }

    private void saveLastAnalysisMs(long ms) {
        SharedPreferences.Editor e = getSharedPreferences(LIMITS_PREFS, MODE_PRIVATE).edit();
        e.putLong(lastAnalysisKey(), ms).apply();
    }

    /** كم ميلي ثانية باقي حتى تقدر تجرب مرة ثانية (0 = جاهز) */
    private long cooldownRemainingMs() {
        long last = getLastAnalysisMs();
        if (last <= 0) return 0;
        long left = FREE_COOLDOWN_MS - (System.currentTimeMillis() - last);
        // لو الساعة رجعت لورا لا نحبس المستخدم أكثر من المدة الكاملة
        return Math.max(0, Math.min(left, FREE_COOLDOWN_MS));
    }

    private String formatRemaining(long ms) {
        long totalMin = (ms + 59_999) / 60_000; // تقريب لفوق
        long h = totalMin / 60, m = totalMin % 60;
        if (h > 0 && m > 0) return getString(R.string.duration_h_m, (int) h, (int) m);
        if (h > 0) return getString(R.string.duration_h, (int) h);
        return getString(R.string.duration_m, (int) m);
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    private void checkPermissionAndStartRecording() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    RECORD_AUDIO_PERMISSION_CODE);
        } else {
            checkDailyLimitAndRecord();
        }
    }

    private void checkDailyLimitAndRecord() {
        AppDatabase db = AppDatabase.getInstance(this);

        AppDatabase.databaseExecutor.execute(() -> {
            User user = db.userDao().getUserById(currentUserId);

            if (user == null) {
                isProcessingRecordRequest = false;
                runOnUiThread(() -> Toast.makeText(this, getString(R.string.toast_user_missing), Toast.LENGTH_SHORT).show());
                return;
            }

            String today = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                    .format(new java.util.Date());

            if (!today.equals(user.getLastAnalysisDate())) {
                user.setDailyAnalysesUsed(0);
                user.setLastAnalysisDate(today);
                db.userDao().updateUser(user); // ⬅️ السطر الناقص — بدونه التصفير ما كان ينحفظ أبداً
            }

            boolean isPremiumOrRegular = !"free".equals(user.getPlanType());

            // الخطة المجانية: لازم فاصل زمني بين كل تحليل والتاني
            if (!isPremiumOrRegular && user.getDailyAnalysesUsed() < 3) {
                long wait = cooldownRemainingMs();
                if (wait > 0) {
                    isProcessingRecordRequest = false;
                    runOnUiThread(() -> Toast.makeText(this,
                            getString(R.string.toast_cooldown_fmt, formatRemaining(wait)),
                            Toast.LENGTH_LONG).show());
                    return;
                }
            }

            if (isPremiumOrRegular || user.getDailyAnalysesUsed() < 3) {
                runOnUiThread(() -> {
                    if (isPremiumOrRegular) {
                        isProcessingRecordRequest = false;
                        startRecording();
                    } else {
                        AdManager adManager = new AdManager();
                        adManager.loadAndShowAd(HomeActivity.this, new AdManager.AdCallback() {
                            @Override
                            public void onAdWatchedSuccessfully() {
                                isProcessingRecordRequest = false;
                                startRecording();
                            }

                            @Override
                            public void onAdFailed() {
                                isProcessingRecordRequest = false;
                                Toast.makeText(HomeActivity.this,
                                        getString(R.string.toast_ad_failed), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });

            } else {
                isProcessingRecordRequest = false;
                runOnUiThread(() ->
                        Toast.makeText(this, getString(R.string.toast_free_exhausted), Toast.LENGTH_LONG).show()
                );
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RECORD_AUDIO_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkDailyLimitAndRecord();
            } else {
                isProcessingRecordRequest = false;
                Toast.makeText(this, getString(R.string.toast_mic_permission), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void startRecording() {
        File outputDir = getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        if (outputDir == null) {
            Toast.makeText(this, getString(R.string.toast_storage_failed), Toast.LENGTH_SHORT).show();
            return;
        }
        File outputFile = new File(outputDir, "pet_recording_" + System.currentTimeMillis() + ".m4a");

        // نفس الـ callback بينستخدم عند الإيقاف اليدوي وعند الإيقاف التلقائي بعد 5 دقايق،
        // عشان التحليل يشتغل بالحالتين (قبل كان بيشتغل بالتلقائي بس)
        recordingCallback = new AudioRecorderHelper.RecordingCallback() {
            @Override
            public void onRecordingStarted() {
                isRecording = true;
                runOnUiThread(() -> {
                    recordButton.setAlpha(0.5f);
                    Toast.makeText(HomeActivity.this, getString(R.string.toast_recording_started), Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onRecordingStopped(String filePath) {
                isRecording = false;
                isAnalyzing = true;
                runOnUiThread(() -> {
                    recordButton.setAlpha(1.0f);
                    Toast.makeText(HomeActivity.this, getString(R.string.toast_analyzing), Toast.LENGTH_SHORT).show();
                });
                analyzeRecording(new File(filePath));
            }

            @Override
            public void onRecordingError(String message) {
                isRecording = false;
                isAnalyzing = false;
                final String shown;
                if ("ERR_TOO_SHORT".equals(message)) {
                    shown = getString(R.string.err_recording_too_short);
                } else if (message != null && message.startsWith("ERR_PREPARE:")) {
                    shown = getString(R.string.err_recording_prepare_fmt, message.substring(12));
                } else {
                    shown = message;
                }
                runOnUiThread(() -> {
                    recordButton.setAlpha(1.0f);
                    Toast.makeText(HomeActivity.this, shown, Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onTick(int secondsRemaining) {}
        };

        audioRecorderHelper.startRecording(outputFile.getAbsolutePath(), recordingCallback);
    }

    private void stopRecording() {
        if (recordingCallback != null) {
            audioRecorderHelper.stopRecording(recordingCallback);
        }
    }

    private void analyzeRecording(File audioFile) {
        // نجيب نوع الحيوان المسجّل عشان التحليل يكون على قده (قطة/كلب/...)
        AppDatabase.databaseExecutor.execute(() -> {
            Pet pet = AppDatabase.getInstance(this).petDao().getFirstPetByOwner(currentUserId);
            runGeminiAnalysis(audioFile, pet != null ? pet.getType() : null);
        });
    }

    private void runGeminiAnalysis(File audioFile, String petType) {
        new GeminiApiHelper().analyzeAudio(audioFile, petType, new GeminiApiHelper.AnalysisCallback() {
            @Override
            public void onSuccess(String resultText) {
                deleteRecording(audioFile);
                android.util.Log.d("GeminiResult", "نجح التحليل: " + resultText);
                try {
                    JSONObject json = new JSONObject(resultText);
                    String mood = json.getString("mood");
                    int confidence = json.getInt("confidence");
                    boolean needsAlert = json.getBoolean("needsAlert");
                    boolean heardAnimal = json.optBoolean("heardAnimal", true);
                    String reason = json.optString("reason", "");

                    if (!heardAnimal) {
                        // ما في صوت حيوان واضح → ما منحفظ نتيجة وهمية
                        runOnUiThread(() -> Toast.makeText(HomeActivity.this,
                                getString(R.string.toast_no_animal),
                                Toast.LENGTH_LONG).show());
                        return;
                    }

                    AppDatabase db = AppDatabase.getInstance(HomeActivity.this);
                    Pet currentPet = db.petDao().getFirstPetByOwner(currentUserId);
                    if (currentPet == null) {
                        runOnUiThread(() -> Toast.makeText(HomeActivity.this, getString(R.string.toast_pet_missing), Toast.LENGTH_SHORT).show());
                        return;
                    }

                    db.analysisResultDao().insertResult(new AnalysisResult(
                            currentPet.getId(), mood, confidence, System.currentTimeMillis(), needsAlert));
                    // التجربة بتنحسب (والفاصل الزمني بيبدأ) فقط لما ينجح التحليل فعلاً
                    recordAnalysisSuccess();

                    runOnUiThread(() -> {
                        if (needsAlert) {
                            Intent intent = new Intent(HomeActivity.this, HealthAlertActivity.class);
                            intent.putExtra("mood", mood);
                            intent.putExtra("confidence", confidence);
                            startActivity(intent);
                        } else {
                            Toast.makeText(HomeActivity.this,
                                    getString(R.string.toast_result_fmt, mood, confidence)
                                            + (reason.isEmpty() ? "" : "\n" + reason), Toast.LENGTH_LONG).show();
                        }
                    });
                } catch (Exception e) {
                    android.util.Log.e("GeminiResult", "فشل قراءة النتيجة: " + resultText, e);
                    runOnUiThread(() ->
                            Toast.makeText(HomeActivity.this, getString(R.string.toast_parse_failed), Toast.LENGTH_SHORT).show());
                } finally {
                    isAnalyzing = false;
                    audioFile.delete(); // ما منحتفظ بالصوت بعد التحليل (خصوصية)
                }
            }

            @Override
            public void onError(String errorMessage) {
                deleteRecording(audioFile);
                android.util.Log.e("GeminiResult", "فشل التحليل: " + errorMessage);
                isAnalyzing = false;
                audioFile.delete();
                runOnUiThread(() -> Toast.makeText(HomeActivity.this, errorMessage, Toast.LENGTH_LONG).show());
            }
        });
    }

    // يُستدعى من خيط الشبكة بعد نجاح التحليل وحفظ النتيجة
    private void recordAnalysisSuccess() {
        AppDatabase db = AppDatabase.getInstance(this);
        User user = db.userDao().getUserById(currentUserId);
        if (user != null) {
            saveLastAnalysisMs(System.currentTimeMillis());
            user.setDailyAnalysesUsed(user.getDailyAnalysesUsed() + 1);
            db.userDao().updateUser(user);
        }
        refreshHomeData();
    }

    private void openLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    @Override
    protected void onDestroy() {
        if (billingManager != null) billingManager.close();
        super.onDestroy();
    }

    private void deleteRecording(File audioFile) {
        if (audioFile != null && audioFile.exists() && !audioFile.delete()) {
            android.util.Log.w("HomeActivity", "Unable to delete temporary recording");
        }
    }
}
