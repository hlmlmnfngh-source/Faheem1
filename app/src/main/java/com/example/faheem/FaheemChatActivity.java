package com.example.faheem;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

public class FaheemChatActivity extends AppCompatActivity {

    // خلّيها true قبل النشر لما تفعّل بوابة الاشتراك (الشات للبريميوم فقط)
    private static final boolean REQUIRE_PREMIUM = true;

    private ImageView backButton;
    private EditText messageEditText;
    private Button sendButton;
    private LinearLayout messagesContainer;

    // سجل المحادثة بصيغة Gemini: [{role, parts:[{text}]}]
    private final JSONArray history = new JSONArray();
    private final GeminiApiHelper gemini = new GeminiApiHelper();
    private boolean waitingForReply = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faheem_chat);

        backButton = findViewById(R.id.backButton);
        messageEditText = findViewById(R.id.messageEditText);
        sendButton = findViewById(R.id.sendButton);
        messagesContainer = findViewById(R.id.messagesContainer);

        backButton.setOnClickListener(v -> finish());

        if (REQUIRE_PREMIUM) checkPremiumOrExit();

        sendButton.setOnClickListener(v -> {
            String userMessage = messageEditText.getText().toString().trim();
            if (userMessage.isEmpty() || waitingForReply) return;

            addBubble(userMessage, true);
            messageEditText.setText("");
            sendToGemini(userMessage);
        });
    }

    private void checkPremiumOrExit() {
        int userId = new SessionManager(this).getUserId();
        AppDatabase.databaseExecutor.execute(() -> {
            User user = AppDatabase.getInstance(this).userDao().getUserById(userId);
            boolean premium = user != null && "premium".equals(user.getPlanType());
            if (!premium) {
                runOnUiThread(() -> {
                    Toast.makeText(this, getString(R.string.chat_premium_only), Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        });
    }

    private void sendToGemini(String userMessage) {
        try {
            history.put(turn("user", userMessage));
        } catch (Exception e) {
            return;
        }

        waitingForReply = true;
        sendButton.setEnabled(false);
        TextView typing = addBubble(getString(R.string.chat_typing), false);

        gemini.chat(history, new GeminiApiHelper.AnalysisCallback() {
            @Override
            public void onSuccess(String resultText) {
                try { history.put(turn("model", resultText)); } catch (Exception ignored) {}
                runOnUiThread(() -> {
                    typing.setText(resultText);
                    finishWaiting();
                });
            }

            @Override
            public void onError(String errorMessage) {
                // نشيل آخر رسالة مستخدم من السجل عشان ما يفسد ترتيب الأدوار
                history.remove(history.length() - 1);
                runOnUiThread(() -> {
                    typing.setText(errorMessage);
                    finishWaiting();
                });
            }
        });
    }

    private void finishWaiting() {
        waitingForReply = false;
        sendButton.setEnabled(true);
        scrollToBottom();
    }

    private JSONObject turn(String role, String text) throws Exception {
        return new JSONObject()
                .put("role", role)
                .put("parts", new JSONArray().put(new JSONObject().put("text", text)));
    }

    private TextView addBubble(String text, boolean fromUser) {
        TextView bubble = new TextView(this);
        bubble.setText(text);
        bubble.setPadding(24, 16, 24, 16);
        bubble.setMaxWidth((int) (getResources().getDisplayMetrics().widthPixels * 0.78f));

        if (fromUser) {
            bubble.setTextColor(Color.WHITE);
            bubble.setBackgroundResource(R.drawable.bg_chat_bubble_user);
        } else {
            bubble.setTextColor(Color.parseColor("#3E3E3E"));
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(Color.parseColor("#E3D5C5")); // لون Sand من التصميم
            bg.setCornerRadius(40f);
            bubble.setBackground(bg);
        }

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = fromUser ? Gravity.END : Gravity.START;
        params.topMargin = 12;
        bubble.setLayoutParams(params);

        messagesContainer.addView(bubble);
        scrollToBottom();
        return bubble;
    }

    private void scrollToBottom() {
        if (messagesContainer.getParent() instanceof ScrollView) {
            ScrollView sv = (ScrollView) messagesContainer.getParent();
            sv.post(() -> sv.fullScroll(ScrollView.FOCUS_DOWN));
        }
    }
}
