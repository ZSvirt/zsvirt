package org.zstack.header.baremetal.pxeserver;

/**
 * Created by GuoYi on 2018-10-11.
 */
public class BaremetalPxeServerCommands {
    public static class AgentCommand {
        public String uuid;
    }

    public static class AgentResponse {
        public boolean success = true;
        public String error;
        public Long totalCapacity;
        public Long availableCapacity;
    }

    public static class PingCmd extends AgentCommand {
        public String dhcpInterface;
        public boolean enabled;
    }

    public static class PingRsp extends AgentResponse {
        public String uuid;
    }

    public static class ConnectCmd extends AgentCommand {
        public String storagePath;
    }

    public static class ConnectRsp extends AgentResponse {

    }

    public static class InitCmd extends AgentCommand {
        public String managementIp;
        public String managementPort;
        public String storagePath;
        public String dhcpInterface;
        public String dhcpRangeBegin;
        public String dhcpRangeEnd;
        public String dhcpRangeNetmask;
    }

    public static class InitRsp extends AgentResponse {

    }

    public static class StartCmd extends AgentCommand {

    }

    public static class StartRsp extends AgentResponse {

    }

    public static class StopCmd extends AgentCommand {

    }

    public static class StopRsp extends AgentResponse {

    }

    public static class CreateBmConfigsCmd extends AgentCommand {
        public String dhcpInterface;       // dhcp interface name of pxeserver
        public String imageUuid;
        public String bmUuid;              // baremetal instance uuid
        public String pxeNicMac;           // baremetal instance pxe boot nic mac address
        public String nicCfgs;             // baremetal instance system nic configs
        public String username;            // baremetal instance system username
        public String password;            // baremetal instance encrypted system password
        public String preconfigurationType;
        public String preconfigurationContent;
        public String preconfigurationMd5sum;
        public String customPreconfigurations;
        public boolean forceInstall = false;
    }

    public static class CreateBmConfigsRsp extends AgentResponse {

    }

    public static class DeleteBmConfigsCmd extends AgentCommand {
        public String pxeNicMac;
    }

    public static class DeleteBmConfigsRsp extends AgentResponse {

    }

    public static class CreateBmNginxProxyCmd extends AgentCommand {
        public String bmUuid;
        public String upstream;
    }

    public static class CreateBmNginxProxyRsp extends AgentResponse {

    }

    public static class DeleteBmNginxProxyCmd extends AgentCommand {
        public String bmUuid;
    }

    public static class DeleteBmNginxProxyRsp extends AgentResponse {

    }

    public static class CreateBmNoVNCProxyCmd extends AgentCommand {
        public String bmUuid;
        public String upstream;
    }

    public static class CreateBmNoVNCProxyRsp extends AgentResponse {

    }

    public static class DeleteBmNoVNCProxyCmd extends AgentCommand {
        public String bmUuid;
    }

    public static class DeleteBmNoVNCProxyRsp extends AgentResponse {

    }

    public static class CreateBmImageCacheCmd extends AgentCommand {
        public String hostname;            // bs hostname
        public String imageUuid;
        public String imageInstallPath;    // image install path on bs
        public String cacheInstallPath;    // cache install path on pxeserver
    }

    public static class CreateBmImageCacheRsp extends AgentResponse {

    }

    public static class DeleteBmImageCacheCmd extends AgentCommand {
        public String imageUuid;
        public String cacheInstallPath;        // cache install path on pxeserver
    }

    public static class DeleteBmImageCacheRsp extends AgentResponse {

    }

    public static class MountBmImageCacheCmd extends AgentCommand {
        public String imageUuid;
        public String cacheInstallPath;        // cache install path on pxeserver
    }

    public static class MountBmImageCacheRsp extends AgentResponse {

    }

    public static class CreateBmDhcpConfigCmd extends AgentCommand {
        public String chassisUuid;
        public String pxeNicMac;
        public String pxeNicIp;
    }

    public static class CreateBmDhcpConfigRsp extends AgentResponse {

    }

    public static class DeleteBmDhcpConfigCmd extends AgentCommand {
        public String chassisUuid;
    }

    public static class DeleteBmDhcpConfigRsp extends AgentResponse {

    }
}
