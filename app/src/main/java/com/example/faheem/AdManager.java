package com.example.faheem;

import android.app.Activity;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

public class AdManager {

    // معرّف إعلان تجريبي (Test Ad Unit ID) من جوجل — استبدله بمعرّفك الحقيقي قبل النشر
    private static final String AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917";

    private RewardedAd rewardedAd;

    public interface AdCallback {
        void onAdWatchedSuccessfully();
        void onAdFailed();
    }

    public void loadAndShowAd(Activity activity, AdCallback callback) {
        AdRequest adRequest = new AdRequest.Builder().build();

        RewardedAd.load(activity, AD_UNIT_ID, adRequest, new RewardedAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull RewardedAd ad) {
                rewardedAd = ad;
                showAd(activity, callback);
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                Log.e("AdManager", "فشل تحميل الإعلان: " + loadAdError.getMessage());
                callback.onAdFailed();
            }
        });
    }

    private void showAd(Activity activity, AdCallback callback) {
        if (rewardedAd == null) {
            callback.onAdFailed();
            return;
        }

        rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                rewardedAd = null;
            }

            @Override
            public void onAdFailedToShowFullScreenContent(AdError adError) {
                rewardedAd = null;
                callback.onAdFailed();
            }
        });

        rewardedAd.show(activity, rewardItem -> {
            // المستخدم شاهد الإعلان كاملاً واستحق المكافأة
            callback.onAdWatchedSuccessfully();
        });
    }
}