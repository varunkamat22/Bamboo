package com.bamboo.config;

import javax.sql.DataSource;
import javax.validation.Validator;

public interface ApplicationConfiguration {
	
	public Validator getValidator();
	
	public DataSource getDataSource();
	
}
