package org.zstack.header.network.service;

import org.zstack.header.message.NeedJsonSchema;

/**
 * @ Author : yh.w
 * @ Date   : Created in 16:17 2020/9/8
 */
public class FirewallCanonicalEvents {
    public static final String FIREWALL_RULE_CHANGED_PATH = "/firewall/rule/changed";

    @NeedJsonSchema
    public static class FirewallRuleChangedData {
        private String virtualRouterUuid;

        public String getVirtualRouterUuid() {
            return virtualRouterUuid;
        }

        public void setVirtualRouterUuid(String virtualRouterUuid) {
            this.virtualRouterUuid = virtualRouterUuid;
        }
    }
}
