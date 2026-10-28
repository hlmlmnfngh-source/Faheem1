package com.example.faheem;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class HealthAlertActivity extends AppCompatActivity {

    private Button findClinicButton;
    private TextView dismissTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_health_alert);

        findClinicButton = findViewById(R.id.findClinicButton);
        dismissTextView = findViewById(R.id.dismissTextView);

        TextView alertResultTitle = findViewById(R.id.alertResultTitle);
        TextView confidenceTextView = findViewById(R.id.confidenceTextView);

        String mood = getIntent().getStringExtra("mood");
        int confidence = getIntent().getIntExtra("confidence", 0);

        alertResultTitle.setText(mood != null ? mood : getString(R.string.health_alert_result_sample));
        confidenceTextView.setText(getString(R.string.alert_confidence_fmt, confidence));

        findClinicButton.setOnClickListener(v -> openNearbyClinicsOnMap());
        dismissTextView.setOnClickListener(v -> finish());
    }

    private void openNearbyClinicsOnMap() {
        // بيفتح خرائط جوجل ويبحث عن "عيادة بيطرية" بالموقع الحالي
        Uri mapUri = Uri.parse("geo:0,0?q=" + Uri.encode(getString(R.string.clinic_search_query)));
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, mapUri);
        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=veterinary+clinic")));
        }
    }
}
