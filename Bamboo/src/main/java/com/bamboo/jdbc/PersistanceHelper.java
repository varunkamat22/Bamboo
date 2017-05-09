package com.bamboo.jdbc;

import java.util.List;
import java.util.Map;

import com.bamboo.core.Resource;
import com.bamboo.core.SearchCriteria;
import com.google.gson.JsonElement;

public interface PersistanceHelper {
	
	public List<Resource> retrieveAll(Resource resource);
	
	public Resource retrieveByID(String id, Resource resource);
	
	public Resource save(Resource resource);
	
	public void delete(String id,  Resource resource);
	
	public List<Resource> retrieveAllWithFilter(Resource resource, SearchCriteria searchCriteria, SearchCriteria sortCriteria, int batchSize, int startIndex);
	
	public Resource update(String id, Resource resource, Map<String, Map<String, List<Object>>> operationsMap);
	
}
