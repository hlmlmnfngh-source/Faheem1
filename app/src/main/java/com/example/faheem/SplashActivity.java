package com.example.faheem;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable goNext = () -> {
        // مستخدم مسجّل دخول من قبل → الرئيسية مباشرة، غير هيك → الشروط ثم الدخول
        boolean loggedIn = new SessionManager(this).getUserId() > 0;
        Intent intent = new Intent(this, loggedIn ? HomeActivity.class : TermsActivity.class);
        startActivity(intent);
        finish();
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // ننتظر 3 ثوانٍ ثم ننتقل
        handler.postDelayed(goNext, 3000);
    }

    @Override
    protected void onDestroy() {
        // لو الشاشة انغلقت قبل الـ 3 ثواني ما نفتح شي بالغلط
        handler.removeCallbacks(goNext);
        super.onDestroy();
    }
}
