package com.bamboo.jdbc;

import java.util.List;

import com.bamboo.core.SearchCriteria;

public interface PersistanceHelper {
	
	public List<Object> retrieveAll(String resourceName, Class resourceType);
	
	public Object retrieveByID(String id, String resourceName, Class resourceType);
	
	public Object save(Object resource, String resourceName, Class resourceType);
	
	public void delete(String id,  String resourceName);
	
	public List<Object> retrieveAllWithFilter(String resourceName, Class resourceType, SearchCriteria searchCriteria, SearchCriteria sortCriteria, int batchSize, int startIndex);
	
	public Object update(String id, String resourceName, Class resourceType, Object resource);
	
}
