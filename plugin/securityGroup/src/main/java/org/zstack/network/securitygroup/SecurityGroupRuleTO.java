package org.zstack.network.securitygroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SecurityGroupRuleTO {
    // public static final String ACTION_CODE_APPLY_RULE = "applyRule";
    // public static final String ACTION_CODE_DELETE_CHAIN = "deleteChain";
    // public static final String ACTION_CODE_DELETE_RULE = "deleteRule";
    // public static final String ACTION_CODE_REFRESH_RULE = "refreshRule";

    private String securityGroupUuid;
    private List<RuleTO> rules;
    // private String actionCode = ACTION_CODE_APPLY_RULE;


    public String getSecurityGroupUuid() {
        return securityGroupUuid;
    }

    public void setSecurityGroupUuid(String securityGroupUuid) {
        this.securityGroupUuid = securityGroupUuid;
    }

    public List<RuleTO> getRules() {
        if (rules == null) {
            rules = new ArrayList<RuleTO>();
        }
        return rules;
    }

    public void setRules(List<RuleTO> rules) {
        this.rules = rules;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("\nrules: %s", rules));
        return sb.toString();
    }

    // @Override
    // public String toString() {
    //     StringBuilder sb = new StringBuilder();
    //     sb.append(String.format("\nvmNicInternalName: %s", vmNicInternalName));
    //     for (RuleTO to : rules) {
    //         sb.append(String.format("\n%s", to.toFullString()));
    //     }
    //     return sb.toString();
    // }
    
    // @Override
    // public int hashCode() {
    //     return vmNicInternalName.hashCode();
    // }
    
    // @Override
    // public boolean equals(Object obj) {
    //     if (!(obj instanceof SecurityGroupRuleTO)) {
    //         return false;
    //     }

    //     SecurityGroupRuleTO rto = (SecurityGroupRuleTO)obj;
    //     return rto.getVmNicInternalName().equals(this.getVmNicInternalName());
    // }
}
