package com.bamboo.rules;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component(value="ruleExecutionEngine")
public class RuleExecutionEngine {
	
	@Autowired(required=false)
	private List<Rule> rules;
	
	public void executeRules(Object resource, String resourceName, String ruleName){
		if(rules.isEmpty())
			return;
		System.out.println("Total rules found - "+rules.size());
		RuleContext ruleContext = formRuleContext(resource, resourceName, ruleName);
		RuleAction ruleAction = new RuleAction();
		for(Rule rule : rules){
			if(rule.shouldRun(ruleContext)){
				System.out.println("Executing rule :: "+rule.getRuleName());
				rule.runRule(ruleContext, ruleAction);
			}
			if(ruleAction.shouldHalt())
				break;
			if(ruleAction.hasError()){
				throw new RuntimeException(ruleAction.getErrorMessage());
			}
		}
	}
	
	private RuleContext formRuleContext(Object resource,  String resourceName, String ruleName){
		return new RuleContext(resource, resourceName, ruleName);
	}
	
}
