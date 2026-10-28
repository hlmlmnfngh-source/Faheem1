package com.example.faheem;

import android.app.Activity;

import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.Task;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public final class GoogleSignInHelper {
    public interface Callback {
        void onSuccess(FirebaseUser user);
        void onError(int messageRes);
    }

    public void signIn(Activity activity, Callback callback) {
        if (BuildConfig.GOOGLE_WEB_CLIENT_ID.isEmpty()) {
            callback.onError(R.string.google_sign_in_not_configured);
            return;
        }
        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                .setAutoSelectEnabled(false)
                .build();
        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();
        CredentialManager.create(activity).getCredentialAsync(activity, request, null,
                ContextCompat.getMainExecutor(activity),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override public void onResult(GetCredentialResponse result) {
                        Credential credential = result.getCredential();
                        if (!GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(credential.getType())) {
                            callback.onError(R.string.google_sign_in_failed);
                            return;
                        }
                        try {
                            GoogleIdTokenCredential googleCredential =
                                    GoogleIdTokenCredential.createFrom(credential.getData());
                            AuthCredential firebaseCredential = GoogleAuthProvider.getCredential(
                                    googleCredential.getIdToken(), null);
                            Task<com.google.firebase.auth.AuthResult> task = FirebaseAuth.getInstance()
                                    .signInWithCredential(firebaseCredential);
                            task.addOnSuccessListener(auth -> callback.onSuccess(auth.getUser()));
                            task.addOnFailureListener(error -> callback.onError(R.string.google_sign_in_failed));
                        } catch (Exception error) {
                            callback.onError(R.string.google_sign_in_failed);
                        }
                    }

                    @Override public void onError(GetCredentialException error) {
                        callback.onError(R.string.google_sign_in_failed);
                    }
                });
    }
}
