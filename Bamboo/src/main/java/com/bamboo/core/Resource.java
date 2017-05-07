package com.bamboo.core;

import com.bamboo.core.SearchCriteria.FilterOperations;

public interface Resource {
	
	public String getResourceName();
	
	public FilterOperations[] getSupportedFilters();
	
	public Class getResourceClass();
	
	public String getId(); 
	
	public void setId(String id);
	
}
