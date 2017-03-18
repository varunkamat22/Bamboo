package com.bamboo.rules;

public class RuleAction {
	
	private boolean shouldHalt;
	private boolean hasError;
	private String errorMessage;
	
	
	public boolean shouldHalt() {
		return shouldHalt;
	}

	public boolean hasError() {
		return hasError;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void haltAndExit(){	
		shouldHalt = true;
	}
	
	public void exitWithError(Exception ex){
		hasError = true;
		errorMessage = ex.getMessage();
	}
	
	public void exitWithMessage(String message){
		hasError = true;
		errorMessage = message;
	}
	
}
