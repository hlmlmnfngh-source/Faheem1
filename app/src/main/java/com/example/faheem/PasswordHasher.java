package com.example.faheem;

import android.util.Base64;
import java.security.SecureRandom;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Password derivation for locally stored accounts. A server-side provider is preferred. */
public final class PasswordHasher {
    private static final int ITERATIONS = 120_000;
    private static final int KEY_LENGTH = 256;
    private PasswordHasher() { }

    public static String hash(String password) {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        byte[] derived = derive(password.toCharArray(), salt, ITERATIONS);
        return ITERATIONS + ":" + Base64.encodeToString(salt, Base64.NO_WRAP) + ":" + Base64.encodeToString(derived, Base64.NO_WRAP);
    }

    public static boolean verify(String password, String encodedHash) {
        try {
            String[] parts = encodedHash.split(":");
            if (parts.length != 3) return false;
            byte[] expected = Base64.decode(parts[2], Base64.NO_WRAP);
            byte[] actual = derive(password.toCharArray(), Base64.decode(parts[1], Base64.NO_WRAP), Integer.parseInt(parts[0]));
            if (actual.length != expected.length) return false;
            int difference = 0;
            for (int i = 0; i < actual.length; i++) difference |= actual[i] ^ expected[i];
            return difference == 0;
        } catch (Exception ignored) { return false; }
    }

    private static byte[] derive(char[] password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, KEY_LENGTH);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (Exception e) { throw new IllegalStateException("Password hashing is unavailable", e); }
        finally { spec.clearPassword(); }
    }
}
