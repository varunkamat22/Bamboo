package com.bamboo.rules;

public class RuleContext {
	
	private String ruleName;
	private Object contextObject;
	private String resourceName;
	
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
	
	
}
