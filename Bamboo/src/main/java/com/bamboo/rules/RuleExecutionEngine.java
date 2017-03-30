package com.bamboo.rules;

import java.util.List;
import javax.annotation.PostConstruct;

public interface RuleExecutionEngine {
	
	public void executeRules(Object resource, String resourceName, String ruleName);
	
	public void executeRules(Object resource, Object originalResource, List<String> changedFields, String resourceName, String ruleName);
	
	public void constructRuleMap();
}
