package org.zstack.sdk;



public class SecurityGroupIngressRuleTO extends org.zstack.sdk.SecurityGroupRuleInventory {

    public java.util.List friendCidrs;
    public void setFriendCidrs(java.util.List friendCidrs) {
        this.friendCidrs = friendCidrs;
    }
    public java.util.List getFriendCidrs() {
        return this.friendCidrs;
    }

}
