package com.example.faheem;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(tableName = "users")
public class User {

    @PrimaryKey(autoGenerate = true)
    private int id;

    @NonNull
    private String firstName;

    @NonNull
    private String lastName;

    @NonNull
    private String email;

    private String password;

    private String passwordHash;
    private boolean verified;

    private String planType;
    private int dailyAnalysesUsed;
    private String lastAnalysisDate;
    private String verificationCode;
    private long codeGeneratedAt;

    // كونستركتر فاضي — هاد يلي Room بتستخدمه لإعادة بناء الكائن من صف موجود
    // بدون أي منطق إضافي (بدون تشفير)، عشان ما تصير مشكلة كل مرة نقرأ من القاعدة
    public User() {
    }

    // كونستركتر إنشاء حساب جديد — @Ignore بتقول لـ Room "تجاهليني، استخدمي الفاضي فوق"
    @Ignore
    public User(@NonNull String firstName, @NonNull String lastName,
                @NonNull String email, @NonNull String password) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.password = null;
        this.passwordHash = PasswordHasher.hash(password);
        this.verified = false;
        this.planType = "free";
        this.dailyAnalysesUsed = 0;
        this.lastAnalysisDate = "";
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    @NonNull
    public String getFirstName() { return firstName; }
    public void setFirstName(@NonNull String firstName) { this.firstName = firstName; }

    @NonNull
    public String getLastName() { return lastName; }
    public void setLastName(@NonNull String lastName) { this.lastName = lastName; }

    @NonNull
    public String getEmail() { return email; }
    public void setEmail(@NonNull String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash)
    { this.passwordHash = passwordHash; }

    public boolean isVerified() {return verified; }
    public void setVerified(boolean verified)
    { this.verified = verified; }

    public String getPlanType() { return planType; }
    public void setPlanType(String planType) { this.planType = planType; }

    public int getDailyAnalysesUsed() { return dailyAnalysesUsed; }
    public void setDailyAnalysesUsed(int dailyAnalysesUsed) { this.dailyAnalysesUsed = dailyAnalysesUsed; }

    public String getLastAnalysisDate() { return lastAnalysisDate; }
    public void setLastAnalysisDate(String lastAnalysisDate) { this.lastAnalysisDate = lastAnalysisDate; }

    public String getVerificationCode() { return verificationCode; }
    public void setVerificationCode(String verificationCode) { this.verificationCode = verificationCode; }

    public long getCodeGeneratedAt() { return codeGeneratedAt; }
    public void setCodeGeneratedAt(long codeGeneratedAt) { this.codeGeneratedAt = codeGeneratedAt; }
}