package org.zstack.network.securitygroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

public class HostRuleTO {
    private List<VmNicSecurityTO> vmNics;
    private Map<String, List<RuleTO>> rules;
    private Map<String, List<RuleTO>> ip6Rules;
    private String hostUuid;
    private String hypervisorType;
    private boolean refreshHost;

    public HostRuleTO() {
        vmNics = new ArrayList<VmNicSecurityTO>();
        rules = new HashMap<String, List<RuleTO>>();
        ip6Rules = new HashMap<String, List<RuleTO>>();
    }
    
    public List<VmNicSecurityTO> getVmNics() {
        return vmNics;
    }

    public void setVmNics(List<VmNicSecurityTO> vmNics) {
        this.vmNics = vmNics;
    }

    public Map<String, List<RuleTO>> getRules() {
        return rules;
    }

    public void setRules(Map<String, List<RuleTO>> rules) {
        this.rules = rules;
    }

    public Map<String, List<RuleTO>> getIp6Rules() {
        return ip6Rules;
    }

    public void setIp6Rules(Map<String, List<RuleTO>> ip6Rules) {
        this.ip6Rules = ip6Rules;
    }

    public String getHostUuid() {
        return hostUuid;
    }
    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }
    public String getHypervisorType() {
        return hypervisorType;
    }
    public void setHypervisorType(String hypervisorType) {
        this.hypervisorType = hypervisorType;
    }
    public boolean isRefreshHost() {
        return refreshHost;
    }
    public void setRefreshHost(boolean refreshHost) {
        this.refreshHost = refreshHost;
    }
}
