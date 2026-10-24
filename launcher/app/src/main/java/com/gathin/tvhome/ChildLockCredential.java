package com.gathin.tvhome;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Offline six-digit parent PIN verifier. Never stores plaintext or accepts the time PIN as fallback. */
final class ChildLockCredential {
    private static final int ROUNDS = 60_000;
    private ChildLockCredential() { }

    static boolean valid(String pin) {
        if (pin == null || pin.length() != 6) return false;
        for (int i = 0; i < pin.length(); i++) if (pin.charAt(i) < '0' || pin.charAt(i) > '9') return false;
        return true;
    }
    static String create(String pin) {
        if (!valid(pin)) throw new IllegalArgumentException("Six ASCII digits required");
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return "v1:" + hex(salt) + ":" + hex(derive(pin, salt));
    }
    static boolean matches(String pin, String record) {
        if (!valid(pin) || record == null) return false;
        String[] parts = record.split(":", -1);
        if (parts.length != 3 || !parts[0].equals("v1") || parts[1].length() != 32 || parts[2].length() != 64) return false;
        try { return MessageDigest.isEqual(unhex(parts[2]), derive(pin, unhex(parts[1]))); }
        catch (IllegalArgumentException | IllegalStateException ignored) { return false; }
    }
    private static byte[] derive(String pin, byte[] salt) {
        char[] chars = pin.toCharArray();
        PBEKeySpec key = new PBEKeySpec(chars, salt, ROUNDS, 256);
        Arrays.fill(chars, '\0');
        try { return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(key).getEncoded(); }
        catch (GeneralSecurityException e) { throw new IllegalStateException("PIN verifier unavailable", e); }
        finally { key.clearPassword(); }
    }
    private static String hex(byte[] data) {
        char[] result = new char[data.length * 2];
        String digits = "0123456789abcdef";
        for (int i = 0; i < data.length; i++) {
            result[i * 2] = digits.charAt((data[i] & 255) >>> 4);
            result[i * 2 + 1] = digits.charAt(data[i] & 15);
        }
        return new String(result);
    }
    private static byte[] unhex(String value) {
        byte[] result = new byte[value.length() / 2];
        for (int i = 0; i < result.length; i++) {
            int a = Character.digit(value.charAt(i * 2), 16), b = Character.digit(value.charAt(i * 2 + 1), 16);
            if (a < 0 || b < 0) throw new IllegalArgumentException("Invalid verifier");
            result[i] = (byte) ((a << 4) | b);
        }
        return result;
    }
}
