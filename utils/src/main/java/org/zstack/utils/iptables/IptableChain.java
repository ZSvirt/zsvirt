package org.zstack.utils.iptables;

import java.util.HashSet;
import java.util.Set;

public class IptableChain {
	private String name;
	private Set<String> rules = new HashSet<String>();
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Set<String> getRules() {
		return rules;
	}
	public void setRules(Set<String> rules) {
		this.rules = rules;
	}
	
	public void addRule(String rule) {
		rules.add(rule);
	}
}
