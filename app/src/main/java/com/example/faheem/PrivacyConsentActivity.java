package com.example.faheem;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class PrivacyConsentActivity extends AppCompatActivity {
    public static final String EXTRA_POLICY_ONLY = "policy_only";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy_consent);

        boolean policyOnly = getIntent().getBooleanExtra(EXTRA_POLICY_ONLY, false);
        ImageView backButton = findViewById(R.id.backButton);
        TextView title = findViewById(R.id.privacyTitle);
        TextView body = findViewById(R.id.privacyBody);
        Button acceptButton = findViewById(R.id.acceptPrivacyButton);

        title.setText(policyOnly ? R.string.privacy_policy_title : R.string.privacy_consent_title);
        body.setText(R.string.privacy_policy_body);
        backButton.setVisibility(policyOnly ? View.VISIBLE : View.GONE);
        backButton.setOnClickListener(v -> finish());
        acceptButton.setText(policyOnly ? R.string.close : R.string.privacy_consent_accept);
        acceptButton.setOnClickListener(v -> {
            if (!policyOnly) PrivacyPreferences.acceptRemoteProcessing(this);
            finish();
        });
    }
}
