package com.bamboo.config;

import javax.validation.Validator;

public interface ApplicationConfiguration {
	
	public Validator getValidator();
	
}
