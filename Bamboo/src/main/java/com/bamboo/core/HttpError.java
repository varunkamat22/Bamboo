package com.bamboo.core;

import org.springframework.http.HttpStatus;

public class HttpError {
	private int errorCode;
	private String errorMessage;
	
	public int getErrorCode() {
		return errorCode;
	}
	
	public void setErrorCode(int errorCode) {
		this.errorCode = errorCode;
	}
	
	public String getErrorMessage() {
		return errorMessage;
	}
	
	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}
	
	public static HttpError get404Error(String errorMessage){
		HttpError httpError = new HttpError();
		httpError.setErrorCode(HttpStatus.NOT_FOUND.value());
		httpError.setErrorMessage(errorMessage);
		return httpError;
	}
}
