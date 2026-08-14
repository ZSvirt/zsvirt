package org.zstack.network.securitygroup;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

public class VmNicSecurityTO {
    public static final String ACTION_CODE_APPLY_CHAIN = "applyChain";
    public static final String ACTION_CODE_DELETE_CHAIN = "deleteChain";
    private String vmNicUuid;
    private String internalName;
    private String mac;
    private List<String> vmNicIps;
    private String ingressPolicy;
    private String egressPolicy;
    private String actionCode = ACTION_CODE_APPLY_CHAIN;
    private Map<String, Integer> securityGroupRefs;

    public VmNicSecurityTO() {
        securityGroupRefs = new HashMap<String, Integer>();
        vmNicIps = new ArrayList<>();
    }

    public String getVmNicUuid() {
        return vmNicUuid;
    }

    public void setVmNicUuid(String vmNicUuid) {
        this.vmNicUuid = vmNicUuid;
    }

    public String getInternalName() {
        return internalName;
    }

    public void setInternalName(String internalName) {
        this.internalName = internalName == null ? "" : internalName;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac == null ? "" : mac;
    }

    public List<String> getVmNicIps(){
        return vmNicIps;
    }

    public void setVmNicIps(List<String> vmNicIps) {
        this.vmNicIps = vmNicIps;
    }

    public String getIngressPolicy() {
        return ingressPolicy;
    }

    public void setIngressPolicy(String ingressPolicy) {
        this.ingressPolicy = ingressPolicy;
    }

    public String getEgressPolicy() {
        return egressPolicy;
    }

    public void setEgressPolicy(String egressPolicy) {
        this.egressPolicy = egressPolicy;
    }

    public String getActionCode() {
        return actionCode;
    }

    public void setActionCode(String actionCode) {
        this.actionCode = actionCode;
    }

    public Map<String, Integer> getSecurityGroupRefs() {
        return securityGroupRefs;
    }

    public void setSecurityGroupRefs(Map<String, Integer> securityGroupRefs) {
        this.securityGroupRefs = securityGroupRefs;
    }
}
