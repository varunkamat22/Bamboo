package com.bamboo.auth;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.annotation.PostConstruct;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.inject.Singleton;

import org.springframework.stereotype.Component;
import org.springframework.util.Base64Utils;

import com.bamboo.core.util.ResourceConstants;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@Component
@Singleton
public class DefaultAuthenticationHandler implements AuthenticationHandler, EncryptionHandler {
	
	private char[] encryptionKey;
	private final byte[] salt = { (byte) 0xde, (byte) 0x33, (byte) 0x10, (byte) 0x12, (byte) 0xde, (byte) 0x33, (byte) 0x10, (byte) 0x12, };
	private Cipher encryptCipher;
	private Cipher decryptCipher;
	private final DateFormat formatter = new SimpleDateFormat(ResourceConstants.DATE_FORMAT);
	
	@PostConstruct
	private void init(){
		if(System.getProperty("encryptionKey") == null)
			throw new RuntimeException("Missing encryption key");
		encryptionKey = System.getProperty("encryptionKey").toCharArray();
		
		try {
			SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
			SecretKey key = keyFactory.generateSecret(new PBEKeySpec(encryptionKey));
			
			encryptCipher = Cipher.getInstance("PBEWithMD5AndDES");
			encryptCipher.init(Cipher.ENCRYPT_MODE, key, new PBEParameterSpec(salt, 20));
		
			decryptCipher = Cipher.getInstance("PBEWithMD5AndDES");
			decryptCipher.init(Cipher.DECRYPT_MODE, key, new PBEParameterSpec(salt, 20));
		} catch (Exception ex) {
			throw new RuntimeException("Unable to set up DefaultAuthenticationHandler");
		}
			
	}

	@Override
	public String generateAuthenticationToken(AuthTokenObject authTokenObject) throws Exception {
		JsonObject authObj = new JsonObject();
		authObj.addProperty("rootObjectId", authTokenObject.getRootObjectId());
		authObj.addProperty("expireBy", formatter.format(authTokenObject.getExpireBy()));
		byte[] bytes = encryptCipher.doFinal(authObj.toString().getBytes("UTF-8"));
		return Base64Utils.encodeToString(bytes);
	}

	@Override
	public boolean validateAuthenticationToken(String authToken) {
		try {
			AuthTokenObject authTokenObject = decryptAuthToken(authToken);
			return authTokenObject.getExpireBy().after(new Date());
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}		
	}

	@Override
	public boolean checkCredentials(UserCredential userCredential) {
		return false;
	}

	@Override
	public AuthTokenObject decryptAuthToken(String authToken) throws Exception {
		String decodedToken = new String(decryptCipher.doFinal(Base64Utils.decodeFromString(authToken)), "UTF-8");
		JsonObject tokenObj = new JsonParser().parse(decodedToken).getAsJsonObject();
		Date expireBy = formatter.parse(tokenObj.get("expireBy").getAsString());
		return new AuthTokenObject(tokenObj.get("rootObjectId").getAsString(), expireBy);
	}

	@Override
	public String encodeString(String inString){
		try{
			byte[] bytes = encryptCipher.doFinal(inString.getBytes("UTF-8"));
			return Base64Utils.encodeToString(bytes);
		}catch(Exception e){
			throw new RuntimeException(e.getMessage());
		}
	}

	@Override
	public String decodeString(String inString) {
		try{
			return new String(decryptCipher.doFinal(Base64Utils.decodeFromString(inString)), "UTF-8");
		}catch(Exception e){
			throw new RuntimeException(e.getMessage());
		}
	}
	
}
