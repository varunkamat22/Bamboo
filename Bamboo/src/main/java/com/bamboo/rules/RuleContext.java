package com.bamboo.rules;

import java.util.List;

public class RuleContext {
	
	private String ruleName;
	private Object contextObject;
	private String resourceName;
	private Object originalResource;
	private List<String> changedFields;
	
	public RuleContext(Object contextObject, String resourceName, String ruleName) {
		this.ruleName = ruleName;
		this.contextObject = contextObject;
		this.resourceName = resourceName;
	}

	public String getRuleName() {
		return ruleName;
	}
	
	public Object getContextObject() {
		return contextObject;
	}

	public String getResourceName() {
		return resourceName;
	}

	public Object getOriginalResource() {
		return originalResource;
	}

	public void setOriginalResource(Object originalResource) {
		this.originalResource = originalResource;
	}

	public List<String> getChangedFields() {
		return changedFields;
	}

	public void setChangedFields(List<String> changedFields) {
		this.changedFields = changedFields;
	}
	
	
	
}
