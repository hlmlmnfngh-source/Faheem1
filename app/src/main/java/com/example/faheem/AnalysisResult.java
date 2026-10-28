package com.example.faheem;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(tableName = "analysis_results")
public class AnalysisResult {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private int petId;

    @NonNull
    private String resultText; // مثلاً: "يبدو متوتراً"

    private int confidencePercentage; // مثلاً: 72

    private long timestamp; // وقت التحليل (Unix time بالمللي ثانية)

    private boolean isHealthAlert; // هل هاد التحليل يستدعي تنبيه صحي؟

    private boolean reviewed; // هل المستخدم راجع التنبيه؟

    public AnalysisResult(int petId, @NonNull String resultText,
                          int confidencePercentage, long timestamp, boolean isHealthAlert) {
        this.petId = petId;
        this.resultText = resultText;
        this.confidencePercentage = confidencePercentage;
        this.timestamp = timestamp;
        this.isHealthAlert = isHealthAlert;
        this.reviewed = false;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getPetId() { return petId; }
    public void setPetId(int petId) { this.petId = petId; }

    @NonNull
    public String getResultText() { return resultText; }
    public void setResultText(@NonNull String resultText) { this.resultText = resultText; }

    public int getConfidencePercentage() { return confidencePercentage; }
    public void setConfidencePercentage(int confidencePercentage) { this.confidencePercentage = confidencePercentage; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean isHealthAlert() { return isHealthAlert; }
    public void setHealthAlert(boolean healthAlert) { isHealthAlert = healthAlert; }

    public boolean isReviewed() { return reviewed; }
    public void setReviewed(boolean reviewed) { this.reviewed = reviewed; }
}