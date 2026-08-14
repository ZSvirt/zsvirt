package org.zstack.network.securitygroup;

import org.zstack.utils.data.Pair;
import org.zstack.utils.network.NetworkUtils;

import java.util.ArrayList;
import java.util.List;

public class SecurityGroupIngressRuleTO extends SecurityGroupRuleInventory {
    private List<String> friendCidrs;
    
    public SecurityGroupIngressRuleTO() {
    }
    
    public SecurityGroupIngressRuleTO(SecurityGroupRuleInventory parent) {
        this.setAllowedCidr(parent.getAllowedCidr());
        this.setCreateDate(parent.getCreateDate());
        this.setEndPort(parent.getEndPort());
        this.setLastOpDate(parent.getLastOpDate());
        this.setProtocol(parent.getProtocol());
        this.setSecurityGroupUuid(parent.getSecurityGroupUuid());
        this.setStartPort(parent.getStartPort());
        this.setType(parent.getType());
        this.setUuid(parent.getUuid());
    }
    
    public List<String> getFriendCidrs() {
        return friendCidrs;
    }

    public void setFriendCidrs(List<String> friendCidrs) {
        this.friendCidrs = friendCidrs;
    }

    public void setFirendCidrsByUnrangedIps(List<String> f) {
        List<Pair<String, String>> ranges = NetworkUtils.findConsecutiveIpRange(f);
        friendCidrs = new ArrayList<String>(ranges.size());
        for (Pair<String, String> r : ranges) {
            if (r.first().equals(r.second())) {
                friendCidrs.add(r.first());
            } else {
                friendCidrs.add(String.format("%s-%s", r.first(), r.second()));
            }
        }
    }
}
