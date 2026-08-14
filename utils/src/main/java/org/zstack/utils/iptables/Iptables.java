package org.zstack.utils.iptables;

import java.util.HashMap;
import java.util.Map;

public class Iptables {
	private Map<String, IptableTable> tables = new HashMap<String, IptableTable>();
	
	public static final String TABLE_FILTER = "filter";
	public static final String TABLE_NAT = "nat";
	public static final String TABLE_MANGLE = "mangle";
	
	public static final String CHAIN_OUTPUT = "OUTPUT";
	public static final String CHAIN_INPUT = "INPUT";
	public static final String CHAIN_FORWARD = "FORWARD";
	public static final String CHAIN_PREROUTING = "PREROUTING";
	public static final String CHAIN_POSTROUTING = "POSTROUTING";
	
	public void addRuleToChain(String tableName, String chainName, String rule) {
		IptableTable table = tables.get(tableName);
		if (table == null) {
			table = new IptableTable();
			tables.put(tableName, table);
		}
		
		IptableChain chain = table.getChain(chainName);
		if (chain == null) {
			chain = new IptableChain();
			table.putChain(chainName, chain);
		}
		
		chain.addRule(rule);
	}
	
	public void filterTableAddRuleToChain(String chainName, String rule) {
		addRuleToChain(TABLE_FILTER, chainName, rule);
	}
	
	public void natTableAddRuleToChain(String chainName, String rule) {
		addRuleToChain(TABLE_NAT, chainName, rule);
	}
	
	public void mangleTableAddRuleToChain(String chainName, String rule) {
		addRuleToChain(TABLE_MANGLE, chainName, rule);
	}
	
	public void filterTableInputChainAddRule(String rule) {
		addRuleToChain(TABLE_FILTER, CHAIN_INPUT, rule);
	}
	
	public void filterTableOutputChainAddRule(String rule) {
		addRuleToChain(TABLE_FILTER, CHAIN_OUTPUT, rule);
	}
	
	public void filterTableForwardChainAddRule(String rule) {
		addRuleToChain(TABLE_FILTER, CHAIN_FORWARD, rule);
	}

	public Map<String, IptableTable> buildTables() {
		return tables;
	}
}
