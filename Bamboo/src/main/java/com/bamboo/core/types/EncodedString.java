package com.bamboo.core.types;

import com.bamboo.auth.EncryptionHandler;

public class EncodedString {
	
	private EncryptionHandler encryptionHandler;
	private String encodedString;

	public EncodedString(String inString, EncryptionHandler encryptionHandler){
		encodedString = encryptionHandler.encodeString(inString);
	}

	public String getEncodedString() {
		return encodedString;
	}
	
	public String getDecodedString(){
		return this.encryptionHandler.decodeString(encodedString);
	}
	
}
