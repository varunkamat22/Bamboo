package com.bamboo.core;

import java.util.Arrays;
import java.util.List;

import com.bamboo.core.SearchCriteria.FilterOperations;

public class FilterTranslatorUtil {
	
	private List<FilterOperations> supportedFilters;
	
	public FilterTranslatorUtil(FilterOperations[] supportedFilters) {
		this.supportedFilters = Arrays.asList(supportedFilters);
	}

	public SearchCriteria translate(SearchCriteria leftOperand, String[] parts) {

		if (parts.length < 3)
			throw new RuntimeException("Incorrect filter condition");
		
		FilterOperations currentFilter = getFilterOperationFromName(parts[1]);
		if(currentFilter == null){
			throw new RuntimeException("Incorrect filter condition");
		}
		if(!supportedFilters.contains(currentFilter)){
			throw new RuntimeException("Unsupported filter - "+ currentFilter);
		}
		
		switch (parts[1]) {
		case "eq":
			return parts.length == 3 ? new SearchCriteria(parts[0], FilterOperations.EQUALS, parts[2])
					: translate(new SearchCriteria(parts[0], FilterOperations.EQUALS, parts[2]),
							Arrays.copyOfRange(parts, 2, parts.length));
		case "con":
			return parts.length == 3 ? new SearchCriteria(parts[0], FilterOperations.CONTAINS, parts[2])
					: translate(new SearchCriteria(parts[0], FilterOperations.CONTAINS, parts[2]),
							Arrays.copyOfRange(parts, 2, parts.length));
		case "sw":
			return parts.length == 3 ? new SearchCriteria(parts[0], FilterOperations.STARTS_WITH, parts[2])
					: translate(new SearchCriteria(parts[0], FilterOperations.STARTS_WITH, parts[2]),
							Arrays.copyOfRange(parts, 2, parts.length));
		case "ew":
			return parts.length == 3 ? new SearchCriteria(parts[0], FilterOperations.ENDS_WITH, parts[2])
					: translate(new SearchCriteria(parts[0], FilterOperations.ENDS_WITH, parts[2]),
							Arrays.copyOfRange(parts, 2, parts.length));
		case "neq":
			return parts.length == 3 ? new SearchCriteria(parts[0], FilterOperations.NOT_EQUALS, parts[2])
					: translate(new SearchCriteria(parts[0], FilterOperations.NOT_EQUALS, parts[2]),
							Arrays.copyOfRange(parts, 2, parts.length));
		case "gt":
			return parts.length == 3 ? new SearchCriteria(parts[0], FilterOperations.GREATER_THAN, parts[2])
					: translate(new SearchCriteria(parts[0], FilterOperations.GREATER_THAN, parts[2]),
							Arrays.copyOfRange(parts, 2, parts.length));
		case "lt":
			return parts.length == 3 ? new SearchCriteria(parts[0], FilterOperations.LESS_THAN, parts[2])
					: translate(new SearchCriteria(parts[0], FilterOperations.LESS_THAN, parts[2]),
							Arrays.copyOfRange(parts, 2, parts.length));
		case "and":
			return new SearchCriteria(leftOperand, FilterOperations.AND,
					translate(null, Arrays.copyOfRange(parts, 2, parts.length)));
		case "or":
			return new SearchCriteria(leftOperand, FilterOperations.OR,
					translate(null, Arrays.copyOfRange(parts, 2, parts.length)));
		default:
			throw new RuntimeException("Incorrect filter condition");
		}

	}
	
	public static FilterOperations getFilterOperationFromName(String filterName){
		switch (filterName) {
		case "eq":
			return FilterOperations.EQUALS;
		case "con":
			return FilterOperations.CONTAINS;
		case "sw":
			return FilterOperations.STARTS_WITH;
		case "ew":
			return FilterOperations.ENDS_WITH;
		case "neq":
			return FilterOperations.NOT_EQUALS;
		case "gt":
			return FilterOperations.GREATER_THAN;
		case "lt":
			return FilterOperations.LESS_THAN;
		case "and":
			return FilterOperations.AND;
		case "or":
			return FilterOperations.OR;
		default:
			throw new RuntimeException("Incorrect filter condition");
		}
	}

}
