package com.bamboo.core;

import java.util.ArrayList;
import java.util.List;

public class HttpError {
	private List<String> errorMessages;
	
	public HttpError(String errorMessage) {
		this.errorMessages = new ArrayList<String>();
		this.addToErrorMessages(errorMessage);
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
	
	
}
