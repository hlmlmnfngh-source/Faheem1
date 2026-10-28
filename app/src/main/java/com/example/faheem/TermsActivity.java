package com.example.faheem;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class TermsActivity extends AppCompatActivity {

    private CheckBox checkboxAgree;
    private Button btnContinue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terms);

        // fitsSystemWindows كان بيلغي الـ padding (24dp) فالعناصر تلزق بحواف الشاشة،
        // فنضيف مسافة أشرطة النظام فوق المسافة الأصلية بدل ما نستبدلها
        final android.view.View root = findViewById(R.id.termsRoot);
        final int pad = (int) (24 * getResources().getDisplayMetrics().density);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(pad + bars.left, pad + bars.top, pad + bars.right, pad + bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        checkboxAgree = findViewById(R.id.checkbox_agree);
        btnContinue = findViewById(R.id.btn_continue);

        checkboxAgree.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                btnContinue.setEnabled(isChecked);
                if (isChecked) {
                    btnContinue.setBackgroundTintList(ColorStateList.valueOf(
                            ContextCompat.getColor(TermsActivity.this, R.color.brand_primary_green)));
                } else {
                    btnContinue.setBackgroundTintList(ColorStateList.valueOf(
                            ContextCompat.getColor(TermsActivity.this, R.color.consent_button_disabled)));
                }
            }
        });

        btnContinue.setOnClickListener(v -> {
            Intent intent = new Intent(TermsActivity.this, FeatureActivity.class);
            startActivity(intent);
            finish();
        });
    }
}