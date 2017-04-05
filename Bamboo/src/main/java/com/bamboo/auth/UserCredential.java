package com.bamboo.auth;

public class UserCredential {
	
	private String userName;
	private char[] password;
	
	public UserCredential(String userName, char[] password) {
		this.userName = userName;
		this.password = password.clone();
	}

	public String getUserName() {
		return userName;
	}

	public char[] getPassword() {
		return password.clone();
	}
	
	public void clear(){
		this.userName = null;
		this.password = null;
	}
	
}
