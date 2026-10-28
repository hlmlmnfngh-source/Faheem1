package com.example.faheem;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class PaymentActivity extends AppCompatActivity {

    private ImageView backButton;
    private Button startTrialButton, subscribePremiumButton;
    private BillingManager billingManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        backButton = findViewById(R.id.backButton);
        startTrialButton = findViewById(R.id.startTrialButton);
        subscribePremiumButton = findViewById(R.id.subscribePremiumButton);
        billingManager = new BillingManager(this, message ->
                runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_LONG).show()));
        billingManager.connect();

        backButton.setOnClickListener(v -> finish());

        startTrialButton.setOnClickListener(v -> billingManager.launchPurchase(this,
                BillingProducts.REGULAR_SUBSCRIPTION));

        subscribePremiumButton.setOnClickListener(v -> billingManager.launchPurchase(this,
                BillingProducts.PREMIUM_SUBSCRIPTION));
    }

    @Override protected void onDestroy() {
        if (billingManager != null) billingManager.close();
        super.onDestroy();
    }
}
