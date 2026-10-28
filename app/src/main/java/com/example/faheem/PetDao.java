package com.example.faheem;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Update;
import androidx.room.Delete;
import androidx.room.Query;

import java.util.List;

@Dao
public interface PetDao {

    // إضافة حيوان جديد لقاعدة البيانات
    @Insert
    void insertPet(Pet pet);

    // تعديل بيانات حيوان موجود
    @Update
    void updatePet(Pet pet);

    // حذف حيوان
    @Delete
    void deletePet(Pet pet);

    // جلب كل الحيوانات الخاصة بمستخدم معيّن (حسب ownerId)
    @Query("SELECT * FROM pets WHERE ownerId = :ownerId")
    List<Pet> getPetsByOwner(int ownerId);

    @Query("SELECT * FROM pets WHERE ownerId = :ownerId ORDER BY id ASC LIMIT 1")
    Pet getFirstPetByOwner(int ownerId);

    // جلب حيوان واحد بالتحديد عن طريق الـ id تبعه
    @Query("SELECT * FROM pets WHERE id = :petId")
    Pet getPetById(int petId);

    @Query("DELETE FROM pets WHERE ownerId = :ownerId")
    void deletePetsByOwner(int ownerId);
}
