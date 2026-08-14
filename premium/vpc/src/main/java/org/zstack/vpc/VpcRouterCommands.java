package org.zstack.vpc;

import org.zstack.network.service.virtualrouter.VirtualRouterCommands;

import java.util.List;

public class VpcRouterCommands {
    public static class VpcRouterSetDnsCmd extends VirtualRouterCommands.AgentCommand {
        private List<String> dns;
        private List<String> nicMac;

        public List<String> getDns() {
            return dns;
        }

        public void setDns(List<String> dns) {
            this.dns = dns;
        }

        public List<String> getNicMac() {
            return nicMac;
        }

        public void setNicMac(List<String> nicMac) {
            this.nicMac = nicMac;
        }
    }

    public static class VpcRouterSetDnsRsp extends VirtualRouterCommands.AgentResponse {
    }

}
