package com.example.faheem;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationsHistoryActivity extends AppCompatActivity {

    private LinearLayout alertsContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications_history);

        ImageView backButton = findViewById(R.id.backButton);
        alertsContainer = findViewById(R.id.alertsContainer);
        backButton.setOnClickListener(v -> finish());
    }

    // نحمّل بـ onResume عشان القائمة تتحدّث لما نرجع من شاشة التنبيه
    @Override
    protected void onResume() {
        super.onResume();
        loadAlerts();
    }

    private void loadAlerts() {
        int userId = new SessionManager(this).getUserId();
        AppDatabase.databaseExecutor.execute(() -> {
            List<AnalysisResult> alerts = AppDatabase.getInstance(this)
                    .analysisResultDao().getHealthAlertsByOwner(userId);
            runOnUiThread(() -> render(alerts));
        });
    }

    private void render(List<AnalysisResult> alerts) {
        alertsContainer.removeAllViews();

        if (alerts.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(getString(R.string.history_empty));
            empty.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(48), 0, 0);
            alertsContainer.addView(empty);
            return;
        }

        SimpleDateFormat fmt = new SimpleDateFormat("d MMM yyyy، h:mm a", Locale.getDefault());
        for (AnalysisResult alert : alerts) {
            alertsContainer.addView(buildCard(alert, fmt));
        }
    }

    private LinearLayout buildCard(AnalysisResult alert, SimpleDateFormat fmt) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setBackgroundResource(R.drawable.bg_alert_history_card);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(10);
        card.setLayoutParams(lp);

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView title = new TextView(this);
        title.setText(alert.getResultText());
        title.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);

        TextView date = new TextView(this);
        date.setText(getString(R.string.history_date_confidence_fmt, fmt.format(new Date(alert.getTimestamp())), alert.getConfidencePercentage()));
        date.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        date.setTextSize(12);

        texts.addView(title);
        texts.addView(date);

        TextView status = new TextView(this);
        status.setTextSize(11);
        status.setText(getString(alert.isReviewed() ? R.string.status_reviewed : R.string.status_pending));
        status.setTextColor(ContextCompat.getColor(this,
                alert.isReviewed() ? R.color.brand_primary_green : R.color.brand_terracotta));

        card.addView(texts);
        card.addView(status);

        card.setOnClickListener(v -> {
            if (!alert.isReviewed()) {
                alert.setReviewed(true);
                AppDatabase.databaseExecutor.execute(() ->
                        AppDatabase.getInstance(this).analysisResultDao().updateResult(alert));
            }
            Intent intent = new Intent(this, HealthAlertActivity.class);
            intent.putExtra("mood", alert.getResultText());
            intent.putExtra("confidence", alert.getConfidencePercentage());
            startActivity(intent);
        });
        return card;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}
