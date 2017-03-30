package com.bamboo.rules;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.inject.Singleton;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("chainedRuleExecutionEngine")
@Singleton
public class ChainedRuleExecutionEngine implements RuleExecutionEngine{
	
	private Map<String, RuleWrapper> ruleMap = new HashMap<>();
	
	@Autowired(required=false)
	private List<Rule> rules;
	
	@PostConstruct 
	public void constructRuleMap(){
		if(rules.isEmpty())
			return;
		System.out.println("Total rules found - "+rules.size());
		
		rules.sort((rule1, rule2) -> rule1.getResourceName().compareTo(rule2.getResourceName()));
		
		RuleWrapper ruleWrapper = null;
		for(Rule rule : rules){
			RuleWrapper temp = new RuleWrapper(rule);
			if(ruleMap.get(rule.getResourceName()) == null){
				//new resource
				ruleMap.put(rule.getResourceName(), temp);
			}else{
				//resource already exists
				ruleWrapper.setNextRuleWrapper(temp);
			}
			ruleWrapper = temp;
			rules = null; //release for gc
		}
	}
	
	public void executeRules(Object resource, String resourceName, String ruleName){
		RuleContext ruleContext = formRuleContext(resource, resourceName, ruleName);
		RuleAction ruleAction = new RuleAction();
		RuleWrapper ruleWrapper = ruleMap.get(resourceName);
		if(ruleWrapper != null){
			ruleWrapper.beginRuleExecution(ruleContext, ruleAction);
		}
	}
	
	private RuleContext formRuleContext(Object resource,  String resourceName, String ruleName){
		return new RuleContext(resource, resourceName, ruleName);
	}

	@Override
	public void executeRules(Object resource, Object originalResource, List<String> changedFields, String resourceName, String ruleName) {
		RuleContext ruleContext = formRuleContext(resource, resourceName, ruleName);
		ruleContext.setOriginalResource(originalResource);
		ruleContext.setChangedFields(changedFields);
		RuleAction ruleAction = new RuleAction();
		RuleWrapper ruleWrapper = ruleMap.get(resourceName);
		if(ruleWrapper != null){
			ruleWrapper.beginRuleExecution(ruleContext, ruleAction);
		}
	}
}
