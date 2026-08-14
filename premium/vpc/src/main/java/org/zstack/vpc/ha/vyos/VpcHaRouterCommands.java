package org.zstack.vpc.ha.vyos;

import org.zstack.network.service.virtualrouter.VirtualRouterCommands;

import java.util.List;

public class VpcHaRouterCommands {
    public static String VYOS_HA_ENABLE_PATH = "/enableVyosha";
    public static String SYNC_VPC_ROUTER_HA_PATH = "/syncVpcRouterHa";
    public static String RESTART_KEEPALIVED_PATH = "/restartKeepalived";

    static public class VyosHaVip{
        public String nicMac;
        public String nicVip;
        public String netmask;
        public String category;
    }

    public static class VyosHaEnableCmd extends VirtualRouterCommands.AgentCommand {
        public Integer keepalive;

        public String heartbeatNic;
        public String peerIp;
        public String localIp;
        public List<String> monitors;
        public List<VyosHaVip> vips;
        public String callbackUrl;

        public Integer getKeepalive() {
            return keepalive;
        }

        public void setKeepalive(Integer keepalive) {
            this.keepalive = keepalive;
        }

        public String getHeartbeatNic() {
            return heartbeatNic;
        }

        public void setHeartbeatNic(String heartbeatNic) {
            this.heartbeatNic = heartbeatNic;
        }

        public String getPeerIp() {
            return peerIp;
        }

        public void setPeerIp(String peerIp) {
            this.peerIp = peerIp;
        }

        public List<String> getMonitors() {
            return monitors;
        }

        public void setMonitors(List<String> monitors) {
            this.monitors = monitors;
        }

        public List<VyosHaVip> getVips() {
            return vips;
        }

        public void setVips(List<VyosHaVip> vips) {
            this.vips = vips;
        }

        public String getLocalIp() {
            return localIp;
        }

        public void setLocalIp(String localIp) {
            this.localIp = localIp;
        }
    }

    public static class VpcRouterHaStatusCmd {
        public String virtualRouterUuid;
        public String haStatus;
    }

    public static class SyncVpcRouterHaCmd extends VirtualRouterCommands.AgentCommand {
    }

    public static class RestartKeepalivedCmd extends VirtualRouterCommands.AgentCommand {
    }

    public static class VyosHaEnableRsp extends VirtualRouterCommands.AgentResponse {
    }

    public static class SyncVpcRouterHaRsp extends VirtualRouterCommands.AgentResponse {
        private String haStatus;

        public String getHaStatus() {
            return haStatus;
        }

        public void setHaStatus(String haStatus) {
            this.haStatus = haStatus;
        }
    }

    public static class RestartKeepalivedRsp extends VirtualRouterCommands.AgentResponse {
    }
}
