package org.zstack.simulator.appliancevm;

import org.zstack.appliancevm.ApplianceVmCommands.InitCmd;
import org.zstack.appliancevm.ApplianceVmFirewallRuleTO;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 */
public class ApplianceVmSimulatorConfig {
    public volatile boolean prepareBootstrapInfoSuccess = true;
    public volatile Map<String, Object> bootstrapInfo;
    public volatile boolean refreshFirewallSuccess = true;
    public volatile List<ApplianceVmFirewallRuleTO> firewallRules = new ArrayList<ApplianceVmFirewallRuleTO>();
    public volatile List<InitCmd> initCmds = new ArrayList<InitCmd>();
}
