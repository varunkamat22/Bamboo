package com.bamboo.rules;

public class RuleWrapper{
	
	private Rule currentRule;
	private RuleWrapper nextRuleWrapper;
	
	public RuleWrapper(Rule currentRule) {
		super();
		this.currentRule = currentRule;
	}

	public void setNextRuleWrapper(RuleWrapper nextRuleWrapper) {
		this.nextRuleWrapper = nextRuleWrapper;
	}

	public void beginRuleExecution(RuleContext ruleContext, RuleAction ruleAction){
		if(currentRule.shouldRun(ruleContext)){
			System.out.println("Executing rule :: "+currentRule.getRuleName());
			currentRule.runRule(ruleContext, ruleAction);
		}
		if(!ruleAction.shouldHalt() && !ruleAction.hasError()){
			if(nextRuleWrapper != null)
				nextRuleWrapper.beginRuleExecution(ruleContext, ruleAction);
		}else if(ruleAction.hasError()){
			throw new RuntimeException(ruleAction.getErrorMessage());
		}
	}

}
