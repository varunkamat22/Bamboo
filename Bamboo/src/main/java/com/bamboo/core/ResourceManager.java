package com.bamboo.core;

import java.util.List;

import com.bamboo.core.SearchCriteria.FilterOperations;

public interface ResourceManager<T>{
	
	public String getResourceName();
	
	public T getFromId(String id);
	
	public List<T> get(SearchCriteria filters, SearchCriteria sortCriteria, int batchSize, int startIndex);
	
	public String save(T t);
	
	public T update(T t);
	
	public void delete(String id);
	
	public FilterOperations[] getSupportedFilters();
	
	public Class getResourceClass();

}
