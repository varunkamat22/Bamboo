package com.bamboo.rules;

import java.util.List;

public interface RuleExecutionEngine {
	
	public void executeRules(Object resource, String resourceName, String ruleName);
	
	public void executeRules(Object resource, Object originalResource, List<String> changedFields, String resourceName, String ruleName);
	
}
