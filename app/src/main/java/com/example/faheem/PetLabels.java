package com.example.faheem;

import android.content.Context;

/**
 * القيم المخزّنة بقاعدة البيانات تبقى ثابتة (بالعربي) عشان ما تنكسر بيانات المستخدمين،
 * وهذا الكلاس يحوّلها لنص بلغة الجهاز وقت العرض فقط.
 */
public final class PetLabels {
    private PetLabels() {}

    public static final String[] TYPE_KEYS = {"قطة", "كلب", "طائر", "أرنب", "حصان", "آخر"};
    private static final int[] TYPE_RES = {
            R.string.pet_type_cat, R.string.pet_type_dog, R.string.pet_type_bird,
            R.string.pet_type_rabbit, R.string.pet_type_horse, R.string.pet_type_other};

    public static final String[] GENDER_KEYS = {"ذكر", "أنثى"};
    private static final int[] GENDER_RES = {R.string.gender_male, R.string.gender_female};

    // شاشة إنشاء الحساب (R.array.animal_types_array): قطة، كلب، حصان
    public static final String[] CREATE_TYPE_KEYS = {"قطة", "كلب", "حصان"};

    public static String type(Context c, String key) {
        for (int i = 0; i < TYPE_KEYS.length; i++)
            if (TYPE_KEYS[i].equals(key)) return c.getString(TYPE_RES[i]);
        return key;
    }

    public static String gender(Context c, String key) {
        for (int i = 0; i < GENDER_KEYS.length; i++)
            if (GENDER_KEYS[i].equals(key)) return c.getString(GENDER_RES[i]);
        return key;
    }

    public static String[] typeLabels(Context c) {
        String[] out = new String[TYPE_KEYS.length];
        for (int i = 0; i < out.length; i++) out[i] = c.getString(TYPE_RES[i]);
        return out;
    }

    public static String[] genderLabels(Context c) {
        String[] out = new String[GENDER_KEYS.length];
        for (int i = 0; i < out.length; i++) out[i] = c.getString(GENDER_RES[i]);
        return out;
    }

    public static int indexOf(String[] keys, String key) {
        for (int i = 0; i < keys.length; i++) if (keys[i].equals(key)) return i;
        return 0;
    }
}
