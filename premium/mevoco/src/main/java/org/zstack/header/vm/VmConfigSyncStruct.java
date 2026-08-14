package org.zstack.header.vm;

import org.zstack.header.log.NoLogging;
import org.zstack.network.service.HostRouteUtils.HostRouteInfo;

import java.io.Serializable;
import java.util.List;

public class VmConfigSyncStruct {
    public static class VmIpConfig {
        private int version;         // v4 or v6
        private String proto;         // dhcp or static
        private String ip;
        private String netmask;
        private List<String> dns;
        private String gateway;
        private List<HostRouteInfo> routes;

        public List<HostRouteInfo> getRoutes() {
            return routes;
        }

        public void setRoutes(List<HostRouteInfo> routes) {
            this.routes = routes;
        }

        public int getVersion() { return version;}

        public void setVersion(int version) {this.version = version;}

        public String getProto() {return proto;}

        public void setProto(String proto) {this.proto = proto;}

        public String getIp() {return ip;}

        public void setIp(String ip) {this.ip = ip;}

        public String getNetmask() {return netmask;}

        public void setNetmask(String netmask) {this.netmask = netmask;}

        public List<String> getDns() {return dns;}

        public void setDns(List<String> dns) {this.dns = dns;}

        public String getGateway() {return gateway;}

        public void setGateway(String gateway) {this.gateway = gateway;}

        @Override
        public String toString() {
            return "VmIpConfig{" +
                    "version=" + version +
                    ", proto='" + proto + '\'' +
                    ", ip='" + ip + '\'' +
                    ", netmask='" + netmask + '\'' +
                    ", dns=" + dns +
                    ", gateway='" + gateway + '\'' +
                    ", routes=" + routes +
                    '}';
        }
    }

    public static class VmPortConfig {
        private List<VmIpConfig> vmIps;
        private String mac;
        private Integer mtu;
        private Boolean isDefault;
        private String haState;

        public List<VmIpConfig> getVmIps() {return vmIps;}

        public void setVmIps(List<VmIpConfig> vmIps) {this.vmIps = vmIps;}

        public String getMac() {return mac;}

        public void setMac(String mac) {this.mac = mac;}

        public Integer getMtu() {return mtu;}

        public void setMtu(Integer mtu) {this.mtu = mtu;}

        public Boolean getDefault() {
            return isDefault;
        }

        public void setDefault(Boolean aDefault) {
            isDefault = aDefault;
        }

        public String getHaState() {
            return this.haState;
        }

        public void setHaState(String haState) {
            this.haState = haState;
        }

        @Override
        public String toString() {
            return "VmPortConfig{" +
                    "vmIps=" + vmIps +
                    ", mac='" + mac + '\'' +
                    ", mtu=" + mtu +
                    ", isDefault=" + isDefault +
                    ", haState='" + haState + '\'' +
                    '}';
        }
    }

    public static class VmUserConfig implements Serializable {
        private String username;
        @NoLogging
        private String password;
        private Boolean active;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public Boolean getActive() {
            return active;
        }

        public void setActive(Boolean active) {
            this.active = active;
        }

        @Override
        public String toString() {
            return "VmUserConfig{" +
                    "username='" + username + '\'' +
                    ", active=" + active +
                    '}';
        }
    }

    public static class VmDomainConfig implements Serializable {
        private String name;
        private String username;
        @NoLogging
        private String password;
        private String ou;


        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getOu() {
            return ou;
        }

        public void setOu(String ou) {
            this.ou = ou;
        }

        @Override
        public String toString() {
            return "VmDomainConfig{" +
                    "name='" + name + '\'' +
                    ", username='" + username + '\'' +
                    ", ou='" + ou + '\'' +
                    '}';
        }
    }

    public static class VmSpecificationConfig implements Serializable {
        private Boolean generateSID;
        private String hostname;
        private String domainMode;
        private VmDomainConfig domain;
        private VmUserConfig user;

        public Boolean getGenerateSID() {
            return generateSID;
        }

        public void setGenerateSID(Boolean generateSID) {
            this.generateSID = generateSID;
        }

        public String getHostname() {
            return hostname;
        }

        public void setHostname(String hostname) {
            this.hostname = hostname;
        }

        public String getDomainMode() {
            return domainMode;
        }

        public void setDomainMode(String domainMode) {
            this.domainMode = domainMode;
        }

        public VmDomainConfig getDomain() {
            return domain;
        }

        public void setDomain(VmDomainConfig domain) {
            this.domain = domain;
        }

        public VmUserConfig getUser() {
            return user;
        }

        public void setUser(VmUserConfig user) {
            this.user = user;
        }

        @Override
        public String toString() {
            return "VmSpecificationConfig{" +
                    "generateSID=" + generateSID +
                    ", hostname='" + hostname + '\'' +
                    ", domainMode='" + domainMode + '\'' +
                    ", domain=" + domain +
                    ", user=" + user +
                    '}';
        }
    }
}
