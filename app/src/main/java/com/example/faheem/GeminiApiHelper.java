package com.example.faheem;

import android.util.Base64;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import org.json.JSONArray;
import org.json.JSONObject;

public class GeminiApiHelper {

    // المفتاح بيجي من local.properties عبر BuildConfig
    private static final String API_KEY = BuildConfig.GEMINI_API_KEY;

    // اسم الموديل بمكان واحد عشان لو جوجل غيّرته تعدله هون بس
    private static final String MODEL = "gemini-3.8-flash";
    private static final String API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/" + MODEL + ":generateContent";

    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)   // الصوت الطويل بياخد وقت
            .writeTimeout(120, TimeUnit.SECONDS)
            .build();

    // لغة ردود الذكاء الاصطناعي = لغة التطبيق الحالية (عربي أو إنجليزي)
    private static String replyLanguage() {
        return "ar".equals(Locale.getDefault().getLanguage()) ? "العربية" : "English";
    }

    public interface AnalysisCallback {
        void onSuccess(String resultText);
        void onError(String errorMessage);
    }

    // ======================= تحليل الصوت =======================
    public void analyzeAudio(File audioFile, AnalysisCallback callback) {
        analyzeAudio(audioFile, null, callback);
    }

    public void analyzeAudio(File audioFile, String petType, AnalysisCallback callback) {
        if (API_KEY.isEmpty()) {
            callback.onError("لم يتم ضبط مفتاح Gemini. أضف GEMINI_API_KEY إلى local.properties.");
            return;
        }
        try {
            byte[] audioBytes = Files.readAllBytes(audioFile.toPath());
            String base64Audio = Base64.encodeToString(audioBytes, Base64.NO_WRAP);
            send(buildAudioRequest(base64Audio, petType), callback);
        } catch (IOException e) {
            callback.onError("فشل قراءة الملف الصوتي: " + e.getMessage());
        } catch (Exception e) {
            callback.onError("خطأ غير متوقع: " + e.getMessage());
        }
    }

    // ======================= محادثة فهيم (نصية) =======================
    // history: مصفوفة من {role: "user"|"model", parts:[{text}]} بالترتيب
    public void chat(JSONArray history, AnalysisCallback callback) {
        if (API_KEY.isEmpty()) {
            callback.onError("لم يتم ضبط مفتاح Gemini. أضف GEMINI_API_KEY إلى local.properties.");
            return;
        }
        try {
            JSONObject body = new JSONObject();
            body.put("contents", history);

            JSONObject sys = new JSONObject();
            sys.put("parts", new JSONArray().put(new JSONObject().put("text",
                    "أنت \"فهيم\"، مساعد ودود لأصحاب الحيوانات الأليفة. تكلم بالعربية بلهجة بسيطة وواضحة. " +
                    "تساعد في فهم سلوك الحيوان وتقدم نصائح عامة للعناية. " +
                    "رُدّ دائماً بلغة " + replyLanguage() + ". " +
                    "ممنوع منعاً باتاً التشخيص الطبي أو وصف أدوية أو علاجات أو جرعات. " +
                    "إذا ذكر المستخدم أي أعراض أو ألم أو تغيّر مقلق، انصحه بزيارة طبيب بيطري بدون تشخيص. " +
                    "اجعل ردودك قصيرة ومفيدة.")));
            body.put("systemInstruction", sys);

            send(body, callback);
        } catch (Exception e) {
            callback.onError("خطأ غير متوقع: " + e.getMessage());
        }
    }

    // ======================= إرسال الطلب =======================
    private void send(JSONObject requestBody, AnalysisCallback callback) {
        RequestBody body = RequestBody.create(
                requestBody.toString(), MediaType.parse("application/json"));

        Request request = new Request.Builder()
                .url(API_URL)
                .header("x-goog-api-key", API_KEY) // بالـ header بدل الرابط عشان ما يظهر المفتاح بالسجلات
                .post(body)
                .build();

        CLIENT.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                callback.onError("فشل الاتصال: " + e.getMessage());
            }

            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {
                try (Response r = response) {
                    String responseString = r.body() != null ? r.body().string() : "";
                    if (r.isSuccessful()) {
                        String text = extractTextFromResponse(responseString);
                        if (text != null) {
                            callback.onSuccess(text);
                        } else {
                            callback.onError("فشل في استخراج النص من الرد");
                        }
                    } else {
                        // نسجل نص الخطأ بالـ Logcat عشان نعرف السبب الحقيقي (مفتاح؟ موديل؟ حصة؟)
                        Log.e("GeminiApiHelper", "HTTP " + r.code() + ": " + responseString);
                        callback.onError("خطأ من الخادم: " + r.code());
                    }
                }
            }
        });
    }

    private JSONObject buildAudioRequest(String base64Audio, String petType) throws Exception {
        String pet = (petType == null || petType.trim().isEmpty()) ? "حيوان أليف" : petType.trim();

        JSONObject textPart = new JSONObject();
        textPart.put("text",
                "استمع بدقة لهذا التسجيل الصوتي. صاحب الحيوان يقول إنه (" + pet + "). " +
                "مهمتك تقدير المزاج المحتمل اعتماداً فقط على ما تسمعه فعلاً في هذا التسجيل بالذات: " +
                "نوع الصوت (مواء، نباح، خرخرة، عواء، زقزقة...)، حدّته، تكراره، طوله، وهل هو هادئ أو متوتر أو متقطع. " +
                "لا تعطِ إجابة عامة أو جاهزة؛ كل تسجيل يختلف عن غيره ونتيجتك لازم تعكس هذا الاختلاف. " +
                "إذا لم تسمع صوت حيوان واضح (صمت، ضجيج، كلام بشر فقط) اجعل heardAnimal=false وconfidence أقل من 20. " +
                "الحقول المطلوبة: " +
                "heardAnimal (true/false)، " +
                "mood: وصف قصير للمزاج بلغة " + replyLanguage() + " (من كلمتين إلى خمس كلمات)، " +
                "confidence: رقم من 0 إلى 100 يعكس وضوح الصوت فعلاً، " +
                "reason: جملة واحدة قصيرة بلغة " + replyLanguage() + " تذكر ما سمعته وجعلك تستنتج هذا المزاج، " +
                "needsAlert: true فقط إذا كان الصوت يوحي بألم أو ضيق شديد أو نمط غير طبيعي يستدعي زيارة طبيب بيطري، وإلا false. " +
                "هذا تقدير تقريبي وليس تشخيصاً طبياً، ولا تذكر أي مرض أو علاج. " +
                "أجب فقط بصيغة JSON: " +
                "{\"heardAnimal\": true, \"mood\": \"...\", \"confidence\": 0, \"reason\": \"...\", \"needsAlert\": false}");

        JSONObject inlineData = new JSONObject();
        inlineData.put("mime_type", "audio/mp4");
        inlineData.put("data", base64Audio);
        JSONObject audioPart = new JSONObject().put("inline_data", inlineData);

        JSONObject content = new JSONObject()
                .put("parts", new JSONArray().put(textPart).put(audioPart));

        JSONObject requestBody = new JSONObject();
        requestBody.put("contents", new JSONArray().put(content));
        // JSON صرف + حرارة متوسطة عشان الردود ما تتكرر حرفياً لكن تبقى منطقية
        requestBody.put("generationConfig", new JSONObject()
                .put("responseMimeType", "application/json")
                .put("temperature", 0.7));
        return requestBody;
    }

    private String extractTextFromResponse(String jsonResponse) {
        try {
            String text = new JSONObject(jsonResponse)
                    .getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text");
            return text.replace("```json", "").replace("```", "").trim();
        } catch (Exception e) {
            Log.e("GeminiApiHelper", "فشل تحليل الرد: " + e.getMessage() + " | " + jsonResponse);
            return null;
        }
    }
}
