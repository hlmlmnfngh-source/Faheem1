package com.example.faheem;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import java.util.Arrays;
import java.util.List;

public class SettingsActivity extends AppCompatActivity {

    public static final String PREFS = "faheem_settings";
    public static final String KEY_NOTIFICATIONS = "notifications_enabled";
    public static final String KEY_DARK_MODE = "dark_mode_enabled";

    private LinearLayout linkedAccountsRow;
    private Spinner animalTypeSpinner, animalGenderSpinner;
    private Switch notificationsSwitch, darkModeSwitch;
    private TextView deleteAccountText;

    private int userId;
    private Pet firstPet;                 // الحيوان اللي بتعدّله هالإعدادات
    private boolean spinnersReady = false; // لتجاهل أول اختيار تلقائي

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        userId = new SessionManager(this).getUserId();

        ImageView backButton = findViewById(R.id.backButton);
        linkedAccountsRow = findViewById(R.id.linkedAccountsRow);
        animalTypeSpinner = findViewById(R.id.animalTypeSpinner);
        animalGenderSpinner = findViewById(R.id.animalGenderSpinner);
        notificationsSwitch = findViewById(R.id.notificationsSwitch);
        darkModeSwitch = findViewById(R.id.darkModeSwitch);
        deleteAccountText = findViewById(R.id.deleteAccountText);

        animalTypeSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, PetLabels.typeLabels(this)));
        animalGenderSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, PetLabels.genderLabels(this)));

        backButton.setOnClickListener(v -> finish());
        linkedAccountsRow.setOnClickListener(v ->
                Toast.makeText(this, getString(R.string.toast_linked_soon), Toast.LENGTH_SHORT).show());

        // الإشعارات: نحفظ القيمة فعلياً
        SharedPreferences prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        notificationsSwitch.setChecked(prefs.getBoolean(KEY_NOTIFICATIONS, true));
        notificationsSwitch.setOnCheckedChangeListener((b, checked) ->
                prefs.edit().putBoolean(KEY_NOTIFICATIONS, checked).apply());

        darkModeSwitch.setChecked(prefs.getBoolean(KEY_DARK_MODE, false));
        darkModeSwitch.setOnCheckedChangeListener((b, checked) -> {
            prefs.edit().putBoolean(KEY_DARK_MODE, checked).apply();
            AppCompatDelegate.setDefaultNightMode(checked
                    ? AppCompatDelegate.MODE_NIGHT_YES
                    : AppCompatDelegate.MODE_NIGHT_NO);
        });

        deleteAccountText.setOnClickListener(v -> confirmDeleteAccount());
        findViewById(R.id.languageRow).setOnClickListener(v -> showLanguageDialog());

        loadFirstPet();
    }

    private void loadFirstPet() {
        AppDatabase.databaseExecutor.execute(() -> {
            Pet pet = AppDatabase.getInstance(this).petDao().getFirstPetByOwner(userId);
            runOnUiThread(() -> {
                firstPet = pet;
                if (pet != null) {
                    animalTypeSpinner.setSelection(PetLabels.indexOf(PetLabels.TYPE_KEYS, pet.getType()));
                    animalGenderSpinner.setSelection(PetLabels.indexOf(PetLabels.GENDER_KEYS, pet.getGender()));
                }
                attachSpinnerListeners();
            });
        });
    }

    private void attachSpinnerListeners() {
        AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!spinnersReady || firstPet == null) return;
                firstPet.setType(PetLabels.TYPE_KEYS[animalTypeSpinner.getSelectedItemPosition()]);
                firstPet.setGender(PetLabels.GENDER_KEYS[animalGenderSpinner.getSelectedItemPosition()]);
                AppDatabase.databaseExecutor.execute(() ->
                        AppDatabase.getInstance(SettingsActivity.this).petDao().updatePet(firstPet));
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        };
        animalTypeSpinner.setOnItemSelectedListener(listener);
        animalGenderSpinner.setOnItemSelectedListener(listener);
        // نفعّل الحفظ بعد ما تخلص أحداث الاختيار الأولية
        animalTypeSpinner.post(() -> spinnersReady = true);
    }

    // اختيار اللغة: "لغة الجهاز" يتبع لغة الجوال تلقائياً، أو نثبّت العربية/الإنجليزية
    private void showLanguageDialog() {
        final String[] tags = {"", "ar", "en"};
        String[] labels = {
                getString(R.string.language_system),
                getString(R.string.language_arabic),
                getString(R.string.language_english)};
        androidx.core.os.LocaleListCompat current = AppCompatDelegate.getApplicationLocales();
        int checked = 0;
        if (!current.isEmpty() && current.get(0) != null) {
            String lang = current.get(0).getLanguage();
            for (int i = 1; i < tags.length; i++) if (tags[i].equals(lang)) checked = i;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.language_dialog_title)
                .setSingleChoiceItems(labels, checked, (d, which) -> {
                    d.dismiss();
                    AppCompatDelegate.setApplicationLocales(
                            androidx.core.os.LocaleListCompat.forLanguageTags(tags[which]));
                })
                .show();
    }

    private void confirmDeleteAccount() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_title)
                .setMessage(R.string.delete_message)
                .setPositiveButton(R.string.delete_confirm, (d, w) -> deleteAccount())
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    private void deleteAccount() {
        AppDatabase.databaseExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            // الترتيب مهم: النتائج ← الحيوانات ← المستخدم
            db.analysisResultDao().deleteResultsByOwner(userId);
            db.petDao().deletePetsByOwner(userId);
            db.userDao().deleteUserById(userId);
            getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply();

            runOnUiThread(() -> {
                new SessionManager(this).clearSession();
                Toast.makeText(this, getString(R.string.toast_account_deleted), Toast.LENGTH_LONG).show();
                Intent intent = new Intent(this, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        });
    }
}
