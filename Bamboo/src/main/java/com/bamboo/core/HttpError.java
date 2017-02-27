package com.bamboo.core;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;

public class HttpError {
	private int errorCode;
	private List<String> errorMessages;
	
	private HttpError(int errorCode) {
		this.errorCode = errorCode;
		this.errorMessages = new ArrayList<String>();
	}

	public int getErrorCode() {
		return errorCode;
	}
	
	public void setErrorCode(int errorCode) {
		this.errorCode = errorCode;
	}
	
	public List<String> getErrorMessage() {
		return errorMessages;
	}
	
	public void setErrorMessage(List<String> errorMessages) {
		this.errorMessages = errorMessages;
	}
	
	public void addToErrorMessages(String errorMessage){
		this.errorMessages.add(errorMessage);
	}
	
	public static HttpError get404Error(){
		HttpError httpError = new HttpError(HttpStatus.NOT_FOUND.value());
		return httpError;
	}
	
	public static HttpError get404Error(String errorMessage){
		HttpError httpError = new HttpError(HttpStatus.NOT_FOUND.value());
		httpError.addToErrorMessages(errorMessage);
		return httpError;
	}
	
	public static HttpError get404Error(List<String> errorMessages){
		HttpError httpError = new HttpError(HttpStatus.BAD_REQUEST.value());
		httpError.setErrorMessage(errorMessages);
		return httpError;
	}
	
}
