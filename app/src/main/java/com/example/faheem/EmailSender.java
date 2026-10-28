package com.example.faheem;

import android.util.Log;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONObject;

public class EmailSender {

    // استبدل هاي القيم بيلي نسختها من حسابك على emailjs.com
    private static final String SERVICE_ID = "service_rs8kmmp";
    private static final String TEMPLATE_ID = "template_ephbkld";
    private static final String PUBLIC_KEY = "80iaJJMHD6UPDyZI-";

    public interface SendCallback {
        void onSuccess();
        void onFailure(String error);
    }

    public void sendVerificationCode(String toEmail, String code, SendCallback callback) {
        try {
            JSONObject templateParams = new JSONObject();
            templateParams.put("email", toEmail);
            templateParams.put("passcode", code);

            JSONObject requestJson = new JSONObject();
            requestJson.put("service_id", SERVICE_ID);
            requestJson.put("template_id", TEMPLATE_ID);
            requestJson.put("user_id", PUBLIC_KEY);
            requestJson.put("template_params", templateParams);

            OkHttpClient client = new OkHttpClient();
            RequestBody body = RequestBody.create(
                    requestJson.toString(), MediaType.parse("application/json"));

            Request request = new Request.Builder()
                    .url("https://api.emailjs.com/api/v1.0/email/send")
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new okhttp3.Callback() {
                @Override
                public void onFailure(okhttp3.Call call, IOException e) {
                    callback.onFailure(e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try (Response r = response) {
                        if (r.isSuccessful()) {
                            callback.onSuccess();
                        } else {
                            String errorBody = r.body() != null ? r.body().string() : "";
                            callback.onFailure("خطأ من الخادم: " + r.code() + " - " + errorBody);
                        }
                    }
                }
            });

        } catch (Exception e) {
            callback.onFailure(e.getMessage());
        }
    }

    // توليد كود عشوائي من 6 أرقام
    public static String generateCode() {
        int code = (int) (Math.random() * 900000) + 100000;
        return String.valueOf(code);
    }
}