package com.bamboo.jdbc;

import com.bamboo.core.SearchCriteria;
import com.bamboo.core.SearchCriteria.FilterOperations;

public interface SearchQueryBuilder {
	
	public String formSearchQuery(SearchCriteria criteria);
	
	public String translateFilterOperation(FilterOperations filterOperation);
	
}
