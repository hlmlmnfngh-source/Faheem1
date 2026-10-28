package com.example.faheem;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.List;

public class ProfileActivity extends AppCompatActivity {


    private ImageView backButton, addPetButton, userProfileImage, editProfileImageButton;
    private LinearLayout upgradeCardLayout, settingsRow, historyRow,
            termsRow, privacyRow, contactRow, logoutRow, petsContainer;
    private TextView userNameText, userEmailText, planText;
    private int userId;

    private final ActivityResultLauncher<PickVisualMediaRequest> pickMedia =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) {
                    userProfileImage.setImageURI(uri);
                    Toast.makeText(this, getString(R.string.toast_photo_updated), Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        userId = new SessionManager(this).getUserId();

        backButton = findViewById(R.id.backButton);
        addPetButton = findViewById(R.id.addPetButton);
        userProfileImage = findViewById(R.id.userProfileImage);
        editProfileImageButton = findViewById(R.id.editProfileImageButton);
        upgradeCardLayout = findViewById(R.id.upgradeCardLayout);
        settingsRow = findViewById(R.id.settingsRow);
        historyRow = findViewById(R.id.historyRow);
        termsRow = findViewById(R.id.termsRow);
        privacyRow = findViewById(R.id.privacyRow);
        contactRow = findViewById(R.id.contactRow);
        logoutRow = findViewById(R.id.logoutRow);
        petsContainer = findViewById(R.id.petsContainer);
        userNameText = findViewById(R.id.userNameText);
        userEmailText = findViewById(R.id.userEmailText);
        planText = findViewById(R.id.planText);

        backButton.setOnClickListener(v -> finish());
        userProfileImage.setOnClickListener(v -> openImagePicker());
        editProfileImageButton.setOnClickListener(v -> openImagePicker());
        addPetButton.setOnClickListener(v -> showAddPetDialog());

        upgradeCardLayout.setOnClickListener(v -> startActivity(new Intent(this, PaymentActivity.class)));
        settingsRow.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        historyRow.setOnClickListener(v -> startActivity(new Intent(this, NotificationsHistoryActivity.class)));
        privacyRow.setOnClickListener(v -> {
            Intent intent = new Intent(this, PrivacyConsentActivity.class);
            intent.putExtra(PrivacyConsentActivity.EXTRA_POLICY_ONLY, true);
            startActivity(intent);
        });

        logoutRow.setOnClickListener(v -> {
            new SessionManager(this).clearSession();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProfile();
    }

    private void loadProfile() {
        AppDatabase.databaseExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            User user = db.userDao().getUserById(userId);
            List<Pet> pets = db.petDao().getPetsByOwner(userId);
            runOnUiThread(() -> {
                if (user != null) {
                    userNameText.setText(user.getFirstName() + " " + user.getLastName());
                    userEmailText.setText(user.getEmail());
                    planText.setText(planLabel(user.getPlanType()));
                }
                renderPets(pets);
            });
        });
    }

    private String planLabel(String plan) {
        if ("premium".equals(plan)) return getString(R.string.plan_label_premium);
        if ("regular".equals(plan)) return getString(R.string.plan_label_regular);
        return getString(R.string.plan_label_free);
    }

    private void renderPets(List<Pet> pets) {
        petsContainer.removeAllViews();
        for (Pet pet : pets) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setGravity(Gravity.CENTER);
            card.setBackgroundResource(R.drawable.card_bg);
            card.setPadding(dp(16), dp(16), dp(16), dp(16));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(130),
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(dp(12));
            card.setLayoutParams(lp);

            ImageView icon = new ImageView(this);
            icon.setBackgroundResource(R.drawable.bg_icon_circle);
            icon.setPadding(dp(12), dp(12), dp(12), dp(12));
            icon.setImageResource(R.drawable.ic_paw);
            icon.setLayoutParams(new LinearLayout.LayoutParams(dp(64), dp(64)));

            TextView name = new TextView(this);
            name.setText(pet.getName());
            name.setTextSize(15);
            name.setTypeface(name.getTypeface(), android.graphics.Typeface.BOLD);
            name.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            name.setPadding(0, dp(8), 0, 0);

            TextView type = new TextView(this);
            type.setText(PetLabels.type(this, pet.getType()) + " • " + PetLabels.gender(this, pet.getGender()));
            type.setTextSize(13);
            type.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));

            card.addView(icon);
            card.addView(name);
            card.addView(type);
            petsContainer.addView(card);
        }
    }

    private void showAddPetDialog() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(24), dp(16), dp(24), 0);

        EditText nameInput = new EditText(this);
        nameInput.setHint(R.string.hint_pet_name);
        Spinner typeSpinner = new Spinner(this);
        typeSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, PetLabels.typeLabels(this)));
        Spinner genderSpinner = new Spinner(this);
        genderSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, PetLabels.genderLabels(this)));

        form.addView(nameInput);
        form.addView(typeSpinner);
        form.addView(genderSpinner);

        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_add_pet_title)
                .setView(form)
                .setPositiveButton(R.string.btn_add, (d, w) -> {
                    String name = nameInput.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(this, getString(R.string.toast_enter_pet_name), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Pet pet = new Pet(name,
                            PetLabels.TYPE_KEYS[typeSpinner.getSelectedItemPosition()],
                            PetLabels.GENDER_KEYS[genderSpinner.getSelectedItemPosition()], userId);
                    AppDatabase.databaseExecutor.execute(() -> {
                        AppDatabase.getInstance(this).petDao().insertPet(pet);
                        runOnUiThread(this::loadProfile);
                    });
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    private void openImagePicker() {
        pickMedia.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}
