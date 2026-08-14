package org.zstack.network.securitygroup;

import java.util.List;

/**
 * Created by MaJin on 2017-06-29.
 */
public class SecurityGroupMembersTO {
    public static final String ACTION_CODE_DELETE_GROUP = "deleteGroup";
    public static final String ACTION_CODE_UPDATE_GROUP_MEMBER = "updateGroup";

    private String securityGroupUuid;
    private List<String> securityGroupVmIps;
    private List<String> securityGroupVmIp6s;
    private String actionCode = ACTION_CODE_UPDATE_GROUP_MEMBER;

    public void setSecurityGroupUuid(String securityGroupUuid) {
        this.securityGroupUuid = securityGroupUuid;
    }

    public void setActionCode(String actionCode) {
        this.actionCode = actionCode;
    }

    public void setSecurityGroupVmIps(List<String> securityGroupVmIps) {
        this.securityGroupVmIps = securityGroupVmIps;
    }

    public String getSecurityGroupUuid() {
        return securityGroupUuid;
    }

    public List<String> getSecurityGroupVmIps() {
        return securityGroupVmIps;
    }

    public String getActionCode() {
        return actionCode;
    }

    public List<String> getSecurityGroupVmIp6s() {
        return securityGroupVmIp6s;
    }

    public void setSecurityGroupVmIp6s(List<String> securityGroupVmIp6s) {
        this.securityGroupVmIp6s = securityGroupVmIp6s;
    }
}
