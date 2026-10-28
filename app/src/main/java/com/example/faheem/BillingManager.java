package com.example.faheem;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;

import java.util.Arrays;
import java.util.List;

public final class BillingManager implements PurchasesUpdatedListener {
    public interface Callback { void onMessage(int stringRes); }

    private final Context context;
    private final BillingClient billingClient;
    private final Callback callback;

    public BillingManager(Context context, Callback callback) {
        this.context = context.getApplicationContext();
        this.callback = callback;
        billingClient = BillingClient.newBuilder(this.context)
                .setListener(this)
                .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
                .build();
    }

    public void connect() {
        if (billingClient.isReady()) {
            refreshEntitlements();
            return;
        }
        billingClient.startConnection(new BillingClientStateListener() {
            @Override public void onBillingSetupFinished(@NonNull BillingResult result) {
                if (result.getResponseCode() == BillingClient.BillingResponseCode.OK) refreshEntitlements();
                else notifyMessage(R.string.billing_unavailable);
            }
            @Override public void onBillingServiceDisconnected() { }
        });
    }

    public void launchPurchase(Activity activity, String productId) {
        if (!billingClient.isReady()) {
            notifyMessage(R.string.billing_unavailable);
            return;
        }
        QueryProductDetailsParams.Product product = QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId).setProductType(BillingClient.ProductType.SUBS).build();
        billingClient.queryProductDetailsAsync(QueryProductDetailsParams.newBuilder()
                .setProductList(Arrays.asList(product)).build(), (result, detailsResult) -> {
            List<ProductDetails> details = detailsResult.getProductDetailsList();
            if (result.getResponseCode() != BillingClient.BillingResponseCode.OK || details.isEmpty()) {
                notifyMessage(R.string.billing_product_unavailable);
                return;
            }
            ProductDetails productDetails = details.get(0);
            ProductDetails.SubscriptionOfferDetails offer = chooseOffer(productDetails);
            if (offer == null) {
                notifyMessage(R.string.billing_product_unavailable);
                return;
            }
            BillingFlowParams.ProductDetailsParams params = BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(productDetails).setOfferToken(offer.getOfferToken()).build();
            billingClient.launchBillingFlow(activity, BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(Arrays.asList(params)).build());
        });
    }

    private ProductDetails.SubscriptionOfferDetails chooseOffer(ProductDetails details) {
        List<ProductDetails.SubscriptionOfferDetails> offers = details.getSubscriptionOfferDetails();
        if (offers == null || offers.isEmpty()) return null;
        if (BillingProducts.REGULAR_SUBSCRIPTION.equals(details.getProductId())) {
            for (ProductDetails.SubscriptionOfferDetails offer : offers) {
                if (offer.getOfferTags().contains("trial")) return offer;
            }
        }
        return offers.get(0);
    }

    public void refreshEntitlements() {
        if (!billingClient.isReady()) return;
        billingClient.queryPurchasesAsync(QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS).build(), (result, purchases) -> {
            if (result.getResponseCode() == BillingClient.BillingResponseCode.OK) applyEntitlements(purchases);
        });
    }

    @Override public void onPurchasesUpdated(@NonNull BillingResult result, List<Purchase> purchases) {
        if (result.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
            applyEntitlements(purchases);
            notifyMessage(R.string.billing_purchase_success);
        } else if (result.getResponseCode() != BillingClient.BillingResponseCode.USER_CANCELED) {
            notifyMessage(R.string.billing_purchase_failed);
        }
    }

    private void applyEntitlements(List<Purchase> purchases) {
        String plan = "free";
        for (Purchase purchase : purchases) {
            if (purchase.getPurchaseState() != Purchase.PurchaseState.PURCHASED) continue;
            if (purchase.getProducts().contains(BillingProducts.PREMIUM_SUBSCRIPTION)) plan = "premium";
            else if (!"premium".equals(plan) && purchase.getProducts().contains(BillingProducts.REGULAR_SUBSCRIPTION)) plan = "regular";
            if (!purchase.isAcknowledged()) acknowledge(purchase);
        }
        String finalPlan = plan;
        int userId = new SessionManager(context).getUserId();
        if (userId > 0) AppDatabase.databaseExecutor.execute(() -> {
            User user = AppDatabase.getInstance(context).userDao().getUserById(userId);
            if (user != null && !finalPlan.equals(user.getPlanType())) {
                user.setPlanType(finalPlan);
                AppDatabase.getInstance(context).userDao().updateUser(user);
            }
        });
    }

    private void acknowledge(Purchase purchase) {
        billingClient.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.getPurchaseToken()).build(), result -> { });
    }

    private void notifyMessage(int resource) { if (callback != null) callback.onMessage(resource); }
    public void close() { billingClient.endConnection(); }
}
