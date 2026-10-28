package com.example.faheem;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.UUID;

public class CreateAccountActivity extends AppCompatActivity {

    private EditText firstNameEditText, lastNameEditText, emailEditText, passwordEditText;
    private Spinner animalTypeSpinner, animalGenderSpinner;
    private CheckBox termsCheckBox;
    private Button createAccountButton;
    private String googleEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_account);

        firstNameEditText = findViewById(R.id.firstNameEditText);
        lastNameEditText = findViewById(R.id.lastNameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        animalTypeSpinner = findViewById(R.id.animalTypeSpinner);
        animalGenderSpinner = findViewById(R.id.animalGenderSpinner);
        termsCheckBox = findViewById(R.id.termsCheckBox);
        createAccountButton = findViewById(R.id.createAccountButton);

        // تعبئة القوائم المنسدلة من مصفوفات strings
        ArrayAdapter<CharSequence> typeAdapter = ArrayAdapter.createFromResource(
                this, R.array.animal_types_array, android.R.layout.simple_spinner_item);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        animalTypeSpinner.setAdapter(typeAdapter);

        ArrayAdapter<CharSequence> genderAdapter = ArrayAdapter.createFromResource(
                this, R.array.animal_genders_array, android.R.layout.simple_spinner_item);
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        animalGenderSpinner.setAdapter(genderAdapter);

        createAccountButton.setEnabled(false);
        termsCheckBox.setOnCheckedChangeListener((buttonView, isChecked) ->
                createAccountButton.setEnabled(isChecked));

        createAccountButton.setOnClickListener(v -> attemptCreateAccount());
        findViewById(R.id.googleButton).setOnClickListener(v -> startGoogleSignIn());
    }

    private void attemptCreateAccount() {
        if (!PrivacyPreferences.hasRemoteProcessingConsent(this)) {
            startActivity(new Intent(this, PrivacyConsentActivity.class));
            return;
        }
        String firstName = firstNameEditText.getText().toString().trim();
        String lastName = lastNameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        // نخزّن القيمة الثابتة (مو النص المترجم) حسب موقع الاختيار
        String animalType = PetLabels.CREATE_TYPE_KEYS[Math.max(0, animalTypeSpinner.getSelectedItemPosition())];
        String animalGender = PetLabels.GENDER_KEYS[Math.max(0, animalGenderSpinner.getSelectedItemPosition())];

        boolean usingGoogle = googleEmail != null && googleEmail.equalsIgnoreCase(email);
        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || (!usingGoogle && password.isEmpty())) {
            Toast.makeText(this, getString(R.string.toast_fill_all), Toast.LENGTH_SHORT).show();
            return;
        }

        AppDatabase db = AppDatabase.getInstance(this);

        AppDatabase.databaseExecutor.execute(() -> {
            User existingUser = db.userDao().getUserByEmail(email);

            if (existingUser != null) {
                runOnUiThread(() ->
                        Toast.makeText(this, getString(R.string.toast_email_exists), Toast.LENGTH_SHORT).show());
                return;
            }

            // إنشاء المستخدم وحفظه
            User newUser = new User(firstName, lastName, email,
                    usingGoogle ? UUID.randomUUID().toString() : password);

            String code = null;
            if (usingGoogle) {
                newUser.setVerified(true);
            } else {
                code = EmailSender.generateCode();
                newUser.setVerificationCode(code);
                newUser.setCodeGeneratedAt(System.currentTimeMillis());
            }

            long newUserId = db.userDao().insertUserAndGetId(newUser);

            // إنشاء أول حيوان وربطه بالمستخدم
            Pet firstPet = new Pet(getString(R.string.default_pet_name), animalType, animalGender, (int) newUserId);
            db.petDao().insertPet(firstPet);

            if (usingGoogle) {
                new SessionManager(CreateAccountActivity.this).startSession((int) newUserId);
                runOnUiThread(() -> {
                    Intent intent = new Intent(CreateAccountActivity.this, HomeActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                });
                return;
            }

// إرسال الكود فعلياً عبر EmailJS
            EmailSender emailSender = new EmailSender();
            emailSender.sendVerificationCode(email, code, new EmailSender.SendCallback() {
                @Override
                public void onSuccess() {
                    runOnUiThread(() -> {
                        Intent intent = new Intent(CreateAccountActivity.this, VerificationActivity.class);
                        intent.putExtra("email", email);
                        startActivity(intent);
                        finish();
                    });
                }

                @Override
                public void onFailure(String error) {
                    runOnUiThread(() ->
                            Toast.makeText(CreateAccountActivity.this, getString(R.string.toast_send_code_failed, error), Toast.LENGTH_LONG).show());
                }
            });
        });
    }

    private void startGoogleSignIn() {
        if (!PrivacyPreferences.hasRemoteProcessingConsent(this)) {
            startActivity(new Intent(this, PrivacyConsentActivity.class));
            return;
        }
        new GoogleSignInHelper().signIn(this, new GoogleSignInHelper.Callback() {
            @Override public void onSuccess(com.google.firebase.auth.FirebaseUser user) {
                if (user == null || user.getEmail() == null) {
                    Toast.makeText(CreateAccountActivity.this, R.string.google_sign_in_failed, Toast.LENGTH_LONG).show();
                    return;
                }
                googleEmail = user.getEmail();
                emailEditText.setText(googleEmail);
                emailEditText.setEnabled(false);
                passwordEditText.setText("");
                passwordEditText.setEnabled(false);
                Toast.makeText(CreateAccountActivity.this, R.string.google_sign_in_continue_registration, Toast.LENGTH_LONG).show();
            }
            @Override public void onError(int messageRes) {
                Toast.makeText(CreateAccountActivity.this, messageRes, Toast.LENGTH_LONG).show();
            }
        });
    }
}
