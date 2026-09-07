package com.wiley.permissions.security.web;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * We need a way to transport some form of credential from the login form to the
 * login realm in such a way that we can look up the user, uniquely, and so
 * that it can't be faked by users posting to the right URL.
 *
 * To do this, we're going to use this little two-way encryption system to encode
 * some unique ID of the user and pass it.
 *
 * @author ttidwell
 */
public class CredentialEncryptionUtility
{
	private final static String ALGORITHM = "AES";

	private final static int BASE_64_OPTIONS = Base64Util.DONT_BREAK_LINES | Base64Util.URL_SAFE;

	public static String getUniqueKey()
	throws NoSuchAlgorithmException
	{
		KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
		SecretKey key = keyGen.generateKey();
		String output = Base64Util.encodeBytes(key.getEncoded(), BASE_64_OPTIONS);
		return output;
	}

	private static SecretKey getKeyFromString(String key)
	{
		byte bytes[] = Base64Util.decode(key, BASE_64_OPTIONS);
		SecretKey output = new SecretKeySpec(bytes, ALGORITHM);
		return output;
	}

	public static String encrypt(String credential, String key)
	throws
		NoSuchAlgorithmException,
		NoSuchPaddingException,
		IllegalBlockSizeException,
		InvalidKeyException,
		BadPaddingException
	{
		Cipher cipher = Cipher.getInstance(ALGORITHM);
		SecretKey realKey = getKeyFromString(key);
		cipher.init(Cipher.ENCRYPT_MODE, realKey);
		byte bytes[] = cipher.doFinal(credential.getBytes());
		String output = Base64Util.encodeBytes(bytes, BASE_64_OPTIONS);
		return output;
	}

	public static String decrypt(String encryptedCredential, String key)
	throws
		NoSuchAlgorithmException,
		NoSuchPaddingException,
		IllegalBlockSizeException,
		InvalidKeyException,
		BadPaddingException
	{
		Cipher cipher = Cipher.getInstance(ALGORITHM);
		SecretKey realKey = getKeyFromString(key);
		cipher.init(Cipher.DECRYPT_MODE, realKey);
		byte credential[] = Base64Util.decode(encryptedCredential, BASE_64_OPTIONS);
		byte bytes[] = cipher.doFinal(credential);
		String output = new String(bytes);
		return output;
	}
}
