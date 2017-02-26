package com.bamboo.core;

public class SearchCriteria {

	public static enum FilterOperations {
		EQUALS, NOT_EQUALS, STARTS_WITH, ENDS_WITH, CONTAINS, GREATER_THAN, LESS_THAN, AND, OR
	}

	private Object leftOperand;
	private FilterOperations filterOperation;
	private Object rightOperand;

	public SearchCriteria(Object leftOperand, FilterOperations filterOperation, Object rightOperand) {
		this.leftOperand = leftOperand;
		this.filterOperation = filterOperation;
		this.rightOperand = rightOperand;
	}

	public Object getLeftOperand() {
		return leftOperand;
	}

	public FilterOperations getFilterOperation() {
		return filterOperation;
	}

	public Object getRightOperand() {
		return rightOperand;
	}

}
