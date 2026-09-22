package com.wiley.sf.common.security;

import java.security.SecureRandom;

import com.wiley.sf.common.codec.MessageDigestUtil;
import com.wiley.sf.common.lang.ArgUtil;

/**
 * Proper use of this class:
 * Every time you create or change the password for a user, generate a random salt string
 * that is at least 30 characters. Also make sure the user's password is at least 8 characters
 * and follows standard guidelines for a "strong" password (later will add a method to this
 * class to determine if a password is "strong").
 * Then encrypt (hash) salt + password string.
 * Store the salt and the encrypted string into the user record.
 * When the user tries to login, take the salt + password they typed in, encrypt it,
 * see if it matches the stored encrypted string.
 * Another thing that should be done (outside the scope of this class) is to limit the number
 * of failed logins a user can have before their account is disabled - alternatively
 * make sure that when they fail to login that there is a time delay to prevent a brute
 * force attack.
 *
 * @since  JDK 1.6
 * @author Steve Markoff
 */
public class PasswordUtil {

    /**
     * Generates a random password that is 10 characters long and
     * consists of alphanumeric characters.
     */
    public static String generateRandomPassword() {
        return generateRandomPasswordOrSalt(10);
    }

    /**
     * Generates a random salt string of 50 alphanumeric characters.
     */
    public static String generateRandomSalt() {
        return generateRandomPasswordOrSalt(50);
    }

    /**
     * Generates a random string of alphanumeric characters of the requested
     * length.
     *
     * Recommended password length is at least 8 characters (10 ideal).
     * Recommended salt length is at least 30 characters (50 ideal).
     */
    public static String generateRandomPasswordOrSalt(int length) {
        // Don't allow the lowercase letter l or the number 1
        // in the password since they can be mistaken for each other.
        // Also don’t allow capital O or the number 0.
        final String chars = "abcdefghijkmnopqrstuvwxyz23456789ABCDEFGHIJKLMNPQRSTUVWXYZ_";
        final int numChars = chars.length();
        StringBuilder sb = new StringBuilder();
        SecureRandom random = new SecureRandom();

        for (int i = 0; i < length; i++) {
            int index = random.nextInt(numChars);  // inclusive, exclusive
            sb.append(chars.charAt(index));
        }

        return sb.toString();
    }

    /**
     * Applies the SHA-256 algorithm to the plain text. Returns the cipher text bytes
     * converted to hex representation. The plain text should ideally be a randomly
     * generated salt string plus a password (instead of just a password).
     *
     * @param plainText  Must be non-blank
     */
    public static String encrypt(String plainText) {
        ArgUtil.notBlank(plainText, "plainText");
        return MessageDigestUtil.getDigest(plainText, MessageDigestUtil.Algorithm.SHA_256);
    }

    /**
     * Returns null if the password is strong, otherwise returns an
     * explanation for why it is not strong.
     */
    public String isStrongPassword(String password) {
        if (password == null || password.length() < 8) {
            return "Must be at least 8 characters long";
        }

        // TODO:
        // contains at least one lowercase, one uppercase, one number, (one symbol)
        // repetition - check two of same char in a row, also stuff like "wordword"
        //  - probably easiest to just say must contain at least 8 unique chars
        // check dictionary words - need a dictionary

        return null;
    }
}
