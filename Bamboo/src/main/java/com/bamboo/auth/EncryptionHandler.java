package com.bamboo.auth;

public interface EncryptionHandler {
	
	public String encodeString(String inString);
	
	public String decodeString(String inString);
	
}
