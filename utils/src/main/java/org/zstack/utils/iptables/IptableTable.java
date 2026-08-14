package org.zstack.utils.iptables;

import java.util.HashMap;
import java.util.Map;

public class IptableTable {
	private String name;
	private Map<String, IptableChain> chains = new HashMap<String, IptableChain>();
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Map<String, IptableChain> getChains() {
		return chains;
	}
	public void setChains(Map<String, IptableChain> chains) {
		this.chains = chains;
	}
	
	public void putChain(String name, IptableChain chain) {
		chains.put(name, chain);
	}
	
	public IptableChain getChain(String name) {
		return chains.get(name);
	}
}
