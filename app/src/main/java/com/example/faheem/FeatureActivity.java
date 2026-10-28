package com.example.faheem;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class FeatureActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_features);

        Button nextButton = findViewById(R.id.nextButton);
        TextView skipTextView = findViewById(R.id.skipTextView);

        nextButton.setOnClickListener(v -> goToLogin());
        skipTextView.setOnClickListener(v -> goToLogin());
    }

    private void goToLogin() {
        Intent intent = new Intent(FeatureActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}