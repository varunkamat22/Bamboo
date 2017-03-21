package com.bamboo.rules;

public interface Rule {
	
	public boolean shouldRun(RuleContext ruleContext);
	
	public void runRule(RuleContext ruleContext, RuleAction ruleAction);
	
	public String getRuleName();
	
	public String getResourceName();
	
}
