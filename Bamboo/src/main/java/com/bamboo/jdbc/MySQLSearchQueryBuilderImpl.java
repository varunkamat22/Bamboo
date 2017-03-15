package com.bamboo.jdbc;

import org.springframework.stereotype.Component;

import com.bamboo.core.SearchCriteria;
import com.bamboo.core.SearchCriteria.FilterOperations;

@Component()
public class MySQLSearchQueryBuilderImpl implements SearchQueryBuilder {

	@Override
	public String formSearchQuery(SearchCriteria criteria) {
		if(!(criteria.getFilterOperation() == FilterOperations.AND || criteria.getFilterOperation() == FilterOperations.OR)){
			return String.format("(%s %s %s)", criteria.getLeftOperand(), translateFilterOperation(criteria.getFilterOperation()), formatRightOperand(criteria.getRightOperand().toString(), criteria.getFilterOperation()));
		}
		String leftQuery = formSearchQuery((SearchCriteria)criteria.getLeftOperand());
		String rightQuery = formSearchQuery((SearchCriteria)criteria.getRightOperand());
		return String.format("(%s %s %s)", leftQuery, translateFilterOperation(criteria.getFilterOperation()), rightQuery);
	}

	@Override
	public String translateFilterOperation(FilterOperations filterOperation) {
		String op = filterOperation.name();
		switch (op) {
			case "EQUALS":
				return "=";
			case "NOT_EQUALS": 
				return "!=";
			case "STARTS_WITH":
				return "like";
			case "ENDS_WITH":
				return "like";
			case "CONTAINS":
				return "like";
			case "GREATER_THAN":
				return ">";
			case "LESS_THAN":
				return "<";
			case "AND":
				return "AND";
			case "OR":
				return "OR";
		}
		return null;
	}
	
	private Object formatRightOperand(String rightOperand, FilterOperations operation) {
		if(operation == FilterOperations.STARTS_WITH)
			return "'"+rightOperand.replaceAll("\"", "")+"%'";
		if(operation == FilterOperations.ENDS_WITH)
			return "'%"+rightOperand.replaceAll("\"", "")+"'";
		if(operation == FilterOperations.CONTAINS)
			return "'%"+rightOperand.replaceAll("\"", "")+"%'";
		return rightOperand;
	}
	
}
