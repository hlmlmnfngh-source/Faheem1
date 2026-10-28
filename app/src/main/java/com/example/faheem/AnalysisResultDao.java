package com.example.faheem;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Update;
import androidx.room.Query;

import java.util.List;

@Dao
public interface AnalysisResultDao {

    @Insert
    void insertResult(AnalysisResult result);

    @Update
    void updateResult(AnalysisResult result);

    // كل تحليلات حيوان معيّن، الأحدث أول (لعرض "آخر التحليلات" بالرئيسية)
    @Query("SELECT * FROM analysis_results WHERE petId = :petId ORDER BY timestamp DESC")
    List<AnalysisResult> getResultsByPet(int petId);

    // بس التنبيهات الصحية (لشاشة سجل التنبيهات)
    @Query("SELECT * FROM analysis_results WHERE isHealthAlert = 1 ORDER BY timestamp DESC")
    List<AnalysisResult> getAllHealthAlerts();

    // تنبيهات مستخدم معيّن فقط (عبر حيواناته)
    @Query("SELECT ar.* FROM analysis_results ar INNER JOIN pets p ON ar.petId = p.id " +
           "WHERE p.ownerId = :ownerId AND ar.isHealthAlert = 1 ORDER BY ar.timestamp DESC")
    List<AnalysisResult> getHealthAlertsByOwner(int ownerId);

    // لحذف الحساب
    @Query("DELETE FROM analysis_results WHERE petId IN (SELECT id FROM pets WHERE ownerId = :ownerId)")
    void deleteResultsByOwner(int ownerId);

    // آخر التحليلات لمستخدم (الرئيسية)
    @Query("SELECT ar.* FROM analysis_results ar INNER JOIN pets p ON ar.petId = p.id " +
           "WHERE p.ownerId = :ownerId ORDER BY ar.timestamp DESC LIMIT :limit")
    List<AnalysisResult> getRecentByOwner(int ownerId, int limit);
}
