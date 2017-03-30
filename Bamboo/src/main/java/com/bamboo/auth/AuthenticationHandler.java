package com.bamboo.auth;

public interface AuthenticationHandler {
	
	public boolean validateAuthenticationToken(String authToken);
	
	public boolean checkCredentials(UserCredential userCredential);
	
	public String generateAuthenticationToken(AuthTokenObject authTokenObject) throws Exception;
	
	public AuthTokenObject decryptAuthToken(String authToken) throws Exception;
	
}
