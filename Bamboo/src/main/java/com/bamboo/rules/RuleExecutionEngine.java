package com.bamboo.rules;

public interface RuleExecutionEngine {
	
	public void executeRules(Object resource, String resourceName, String ruleName);
	
}
