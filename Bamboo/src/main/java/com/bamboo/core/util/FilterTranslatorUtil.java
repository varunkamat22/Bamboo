package com.bamboo.core.util;

import java.util.Arrays;
import java.util.List;

import com.bamboo.core.SearchCriteria;
import com.bamboo.core.SearchCriteria.FilterOperations;

public class FilterTranslatorUtil {
	
	private List<FilterOperations> supportedFilters;
	
	public FilterTranslatorUtil(FilterOperations[] supportedFilters) {
		this.supportedFilters = Arrays.asList(supportedFilters);
	}

	public SearchCriteria translateSearchQuery(SearchCriteria leftOperand, String[] parts) {

		if (parts.length < 3)
			throw new RuntimeException(ResourceConstants.INVALID_SEARCH_FILTER);
		
		FilterOperations currentFilter = getFilterOperationFromName(parts[1]);
		if(currentFilter == null){
			throw new RuntimeException(ResourceConstants.INVALID_SEARCH_FILTER);
		}
		if(!supportedFilters.contains(currentFilter)){
			throw new RuntimeException(String.format(ResourceConstants.UNSUPPORTED_FILTER, currentFilter));
		}
		
		switch (parts[1]) {
			case ResourceConstants.FILTER_EQUALS:
				return parts.length == 3 ? new SearchCriteria(parts[0], FilterOperations.EQUALS, parts[2])
						: translateSearchQuery(new SearchCriteria(parts[0], FilterOperations.EQUALS, parts[2]),
								Arrays.copyOfRange(parts, 2, parts.length));
			case ResourceConstants.FILTER_CONTAINS:
				return parts.length == 3 ? new SearchCriteria(parts[0], FilterOperations.CONTAINS, parts[2])
						: translateSearchQuery(new SearchCriteria(parts[0], FilterOperations.CONTAINS, parts[2]),
								Arrays.copyOfRange(parts, 2, parts.length));
			case ResourceConstants.FILTER_STARTS_WITH:
				return parts.length == 3 ? new SearchCriteria(parts[0], FilterOperations.STARTS_WITH, parts[2])
						: translateSearchQuery(new SearchCriteria(parts[0], FilterOperations.STARTS_WITH, parts[2]),
								Arrays.copyOfRange(parts, 2, parts.length));
			case ResourceConstants.FILTER_ENDS_WITH:
				return parts.length == 3 ? new SearchCriteria(parts[0], FilterOperations.ENDS_WITH, parts[2])
						: translateSearchQuery(new SearchCriteria(parts[0], FilterOperations.ENDS_WITH, parts[2]),
								Arrays.copyOfRange(parts, 2, parts.length));
			case ResourceConstants.FILTER_NOT_EQUALS:
				return parts.length == 3 ? new SearchCriteria(parts[0], FilterOperations.NOT_EQUALS, parts[2])
						: translateSearchQuery(new SearchCriteria(parts[0], FilterOperations.NOT_EQUALS, parts[2]),
								Arrays.copyOfRange(parts, 2, parts.length));
			case ResourceConstants.FILTER_GREATER_THAN:
				return parts.length == 3 ? new SearchCriteria(parts[0], FilterOperations.GREATER_THAN, parts[2])
						: translateSearchQuery(new SearchCriteria(parts[0], FilterOperations.GREATER_THAN, parts[2]),
								Arrays.copyOfRange(parts, 2, parts.length));
			case ResourceConstants.FILTER_LESS_THAN:
				return parts.length == 3 ? new SearchCriteria(parts[0], FilterOperations.LESS_THAN, parts[2])
						: translateSearchQuery(new SearchCriteria(parts[0], FilterOperations.LESS_THAN, parts[2]),
								Arrays.copyOfRange(parts, 2, parts.length));
			case ResourceConstants.FILTER_AND:
				return new SearchCriteria(leftOperand, FilterOperations.AND,
						  translateSearchQuery(null, Arrays.copyOfRange(parts, 2, parts.length)));
			case ResourceConstants.FILTER_OR:
				return new SearchCriteria(leftOperand, FilterOperations.OR,
						  translateSearchQuery(null, Arrays.copyOfRange(parts, 2, parts.length)));
			default:
				throw new RuntimeException(ResourceConstants.INVALID_SEARCH_FILTER);
		}

	}
	
	public static FilterOperations getFilterOperationFromName(String filterName){
		switch (filterName) {
			case ResourceConstants.FILTER_EQUALS:
				return FilterOperations.EQUALS;
			case ResourceConstants.FILTER_CONTAINS:
				return FilterOperations.CONTAINS;
			case ResourceConstants.FILTER_STARTS_WITH:
				return FilterOperations.STARTS_WITH;
			case ResourceConstants.FILTER_ENDS_WITH:
				return FilterOperations.ENDS_WITH;
			case ResourceConstants.FILTER_NOT_EQUALS:
				return FilterOperations.NOT_EQUALS;
			case ResourceConstants.FILTER_GREATER_THAN:
				return FilterOperations.GREATER_THAN;
			case ResourceConstants.FILTER_LESS_THAN:
				return FilterOperations.LESS_THAN;
			case ResourceConstants.FILTER_AND:
				return FilterOperations.AND;
			case ResourceConstants.FILTER_OR:
				return FilterOperations.OR;
			default:
				throw new RuntimeException(ResourceConstants.INVALID_SEARCH_FILTER);
		}
	}
	
	public static SearchCriteria translateSortQuery(String sortCondition){
		String[] parts = sortCondition.split(ResourceConstants.FILTER_SEPERATOR);
		if(parts.length < 2 || !(parts[1].equals(ResourceConstants.SORT_ORDER_ASC) || parts[1].equals(ResourceConstants.SORT_ORDER_DESC))){
			throw new RuntimeException(ResourceConstants.INVALID_SORT_FILTER);
		}
		return new SearchCriteria(parts[0], FilterOperations.SORTBY, parts[1]);
	}
	
}
