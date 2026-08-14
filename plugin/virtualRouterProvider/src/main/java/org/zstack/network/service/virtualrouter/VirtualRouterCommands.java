package org.zstack.network.service.virtualrouter;

import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.service.vip.VipInventory;
import org.zstack.network.service.virtualrouter.eip.EipTO;
import org.zstack.network.service.virtualrouter.portforwarding.PortForwardingRuleTO;
import org.zstack.utils.gson.JSONObjectUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VirtualRouterCommands {
	public static class AgentCommand implements Serializable {
	}
	
	public static class AgentResponse {
		public boolean success = true;
		public String error;
		public boolean isSuccess() {
			return success;
		}
		public void setSuccess(boolean success) {
			this.success = success;
		}
		public String getError() {
			return error;
		}
		public void setError(String error) {
			this.error = error;
		}
	}

	public static class GetTypeCommand extends AgentCommand{
		private String uuid;

		public String getUuid() {
			return uuid;
		}

		public void setUuid(String uuid) {
			this.uuid = uuid;
		}
	}

	public static class GetTypeRsp extends AgentResponse {
		private boolean isVyos;

		public boolean isVyos() {
			return isVyos;
		}

		public void setVyos(boolean vyos) {
			isVyos = vyos;
		}
	}

    public static class InitCommand extends AgentCommand {
		private String uuid;
        private int restartDnsmasqAfterNumberOfSIGUSER1;
		private String mgtCidr;
		private String logLevel;
		private List<String> timeServers;
		private Map<String,String> parms;

		public String getUuid() {
			return uuid;
		}

		public void setUuid(String uuid) {
			this.uuid = uuid;
		}

		public int getRestartDnsmasqAfterNumberOfSIGUSER1() {
            return restartDnsmasqAfterNumberOfSIGUSER1;
        }

        public void setRestartDnsmasqAfterNumberOfSIGUSER1(int restartDnsmasqAfterNumberOfSIGUSER1) {
            this.restartDnsmasqAfterNumberOfSIGUSER1 = restartDnsmasqAfterNumberOfSIGUSER1;
        }

		public String getMgtCidr() {
			return mgtCidr;
		}

		public void setMgtCidr(String mgtCidr) {
			this.mgtCidr = mgtCidr;
		}

		public String getLogLevel() {
			return logLevel;
		}

		public void setLogLevel(String logLevel) {
			this.logLevel = logLevel;
		}

		public List<String> getTimeServers() {
			return timeServers;
		}

		public void setTimeServers(List<String> timeServers) {
			this.timeServers = timeServers;
		}

		public Map<String, String> getParms() {
			return parms;
		}

		public void setParms(Map<String, String> parms) {
			this.parms = parms;
		}
	}

    public static class InitRsp extends AgentResponse {
		private String zvrVersion;
		private String vyosVersion;
		private String kernelVersion;
		private String ipsecCurrentVersion;
		private String ipsecLatestVersion;

		public String getZvrVersion() {
			return zvrVersion;
		}

		public void setZvrVersion(String zvrVersion) {
			this.zvrVersion = zvrVersion;
		}

		public String getVyosVersion() {
			return vyosVersion;
		}

		public void setVyosVersion(String vyosVersion) {
			this.vyosVersion = vyosVersion;
		}

		public String getKernelVersion() {
			return kernelVersion;
		}

		public void setKernelVersion(String kernelVersion) {
			this.kernelVersion = kernelVersion;
		}

		public String getIpsecCurrentVersion() {
			return ipsecCurrentVersion;
		}

		public void setIpsecCurrentVersion(String ipsecCurrentVersion) {
			this.ipsecCurrentVersion = ipsecCurrentVersion;
		}

		public String getIpsecLatestVersion() {
			return ipsecLatestVersion;
		}

		public void setIpsecLatestVersion(String ipsecLatestVersion) {
			this.ipsecLatestVersion = ipsecLatestVersion;
		}

	}
	
	public static class NicInfo {
		private String ip;
		private String mac;
		private String gateway;
		private String netmask;
		private boolean isDefaultRoute;
		private String category;
		private String physicalInterface;
		private String l2type;
		private Integer vni;
		private String firewallDefaultAction;
		private Integer mtu;
		private String ip6;
		private Integer prefixLength;
		private String gateway6;
		private String addressMode;
		private String state;
		
		public String getIp() {
			return ip;
		}
		public void setIp(String ip) {
			this.ip = ip;
		}
		public String getMac() {
			return mac;
		}
		public void setMac(String mac) {
			this.mac = mac;
		}
		public String getGateway() {
			return gateway;
		}
		public void setGateway(String gateway) {
			this.gateway = gateway;
		}
		public String getNetmask() {
			return netmask;
		}
		public void setNetmask(String netmask) {
			this.netmask = netmask;
		}
		public boolean isDefaultRoute() {
			return isDefaultRoute;
		}
		public void setDefaultRoute(boolean isDefaultRoute) {
			this.isDefaultRoute = isDefaultRoute;
		}

		public String getCategory() {
			return category;
		}

		public void setCategory(String category) {
			this.category = category;
		}

		public String getL2type() {
			return l2type;
		}

		public void setL2type(String l2type) {
			this.l2type = l2type;
		}

		public Integer getVni() {
			return vni;
		}

		public void setVni(Integer vni) {
			this.vni = vni;
		}

		public String getPhysicalInterface() {
			return physicalInterface;
		}

		public void setPhysicalInterface(String physicalInterface) {
			this.physicalInterface = physicalInterface;
		}

		public String getFirewallDefaultAction() {
			return firewallDefaultAction;
		}

		public void setFirewallDefaultAction(String firewallDefaultAction) {
			this.firewallDefaultAction = firewallDefaultAction;
		}

		public Integer getMtu() {
			return mtu;
		}

		public void setMtu(Integer mtu) {
			this.mtu = mtu;
		}

		public String getIp6() {
			return ip6;
		}

		public void setIp6(String ip6) {
			this.ip6 = ip6;
		}

		public Integer getPrefixLength() {
			return prefixLength;
		}

		public void setPrefixLength(Integer prefixLength) {
			this.prefixLength = prefixLength;
		}

		public String getGateway6() {
			return gateway6;
		}

		public void setGateway6(String gateway6) {
			this.gateway6 = gateway6;
		}

		public String getAddressMode() {
			return addressMode;
		}

		public void setAddressMode(String addressMode) {
			this.addressMode = addressMode;
		}

		public String getState() {
			return state;
		}

		public void setState(String state) {
			this.state = state;
		}
	}
	
	public static class ConfigureNicCmd extends AgentCommand {
		private List<NicInfo> nics;

		public List<NicInfo> getNics() {
			return nics;
		}

		public void setNics(List<NicInfo> nics) {
			this.nics = nics;
		}
	}
	
	public static class ConfigureNicRsp extends AgentResponse {
	}

	public static class ConfigureNicFirewallDefaultActionCmd extends AgentCommand {
		private List<NicInfo> nics;

		public List<NicInfo> getNics() {
			return nics;
		}

		public void setNics(List<NicInfo> nics) {
			this.nics = nics;
		}
	}

	public static class ConfigureNicFirewallDefaultActionRsp extends AgentResponse {
	}

	public static class RemoveNicCmd extends AgentCommand {
		private List<NicInfo> nics;

		public List<NicInfo> getNics() {
			return nics;
		}

		public void setNics(List<NicInfo> nics) {
			this.nics = nics;
		}
	}

	public static class RemoveNicRsp extends AgentResponse {
	}
	
	public static class DhcpInfo {
		private String ip;
		private String mac;
		private String netmask;
		private String gateway;
		private List<String> dns;
		private String hostname;
        private String vrNicMac;
        private String dnsDomain;
        private boolean isDefaultL3Network;
		private Integer mtu;

        public String getDnsDomain() {
            return dnsDomain;
        }

        public void setDnsDomain(String domain) {
            this.dnsDomain = domain;
        }

        public boolean isDefaultL3Network() {
            return isDefaultL3Network;
        }

        public void setDefaultL3Network(boolean isDefaultL3Network) {
            this.isDefaultL3Network = isDefaultL3Network;
        }

        public String getVrNicMac() {
            return vrNicMac;
        }

        public void setVrNicMac(String vrNicMac) {
            this.vrNicMac = vrNicMac;
        }

        public String getIp() {
			return ip;
		}
		public void setIp(String ip) {
			this.ip = ip;
		}
		public String getMac() {
			return mac;
		}
		public void setMac(String mac) {
			this.mac = mac;
		}
		public String getNetmask() {
			return netmask;
		}
		public void setNetmask(String netmask) {
			this.netmask = netmask;
		}
		public String getGateway() {
			return gateway;
		}
		public void setGateway(String gateway) {
			this.gateway = gateway;
		}
		public List<String> getDns() {
			return dns;
		}
		public void setDns(List<String> dns) {
			this.dns = dns;
		}
		public String getHostname() {
			return hostname;
		}
		public void setHostname(String hostname) {
			this.hostname = hostname;
		}
		public Integer getMtu() {
			return mtu;
		}
		public void setMtu(Integer mtu) {
			this.mtu = mtu;
		}

	}

	public static class DhcpServerInfo {
		private String 	nicMac;
		private String 	subnet;
		private String 	netmask;
		private String 	gateway;
		private String 	dnsDomain;
		private String 	dnsServer;
		private Integer mtu;
		private List<DhcpInfo> dhcpInfos;

		public String getNicMac() {
			return nicMac;
		}

		public void setNicMac(String nicMac) {
			this.nicMac = nicMac;
		}

		public String getSubnet() {
			return subnet;
		}

		public void setSubnet(String subnet) {
			this.subnet = subnet;
		}

		public String getNetmask() {
			return netmask;
		}

		public void setNetmask(String netmask) {
			this.netmask = netmask;
		}

		public String getGateway() {
			return gateway;
		}

		public void setGateway(String gateway) {
			this.gateway = gateway;
		}

		public String getDnsDomain() {
			return dnsDomain;
		}

		public void setDnsDomain(String dnsDomain) {
			this.dnsDomain = dnsDomain;
		}

		public Integer getMtu() {
			return mtu;
		}

		public void setMtu(Integer mtu) {
			this.mtu = mtu;
		}

		public List<DhcpInfo> getDhcpInfos() {
			return dhcpInfos;
		}

		public void setDhcpInfos(List<DhcpInfo> dhcpInfos) {
			this.dhcpInfos = dhcpInfos;
		}

		public String getDnsServer() {
			return dnsServer;
		}

		public void setDnsServer(String dnsServer) {
			this.dnsServer = dnsServer;
		}
	}

	public static class RemoveDhcpEntryCmd extends AgentCommand {
		private List<DhcpInfo> dhcpEntries;

		public List<DhcpInfo> getDhcpEntries() {
			return dhcpEntries;
		}

		public void setDhcpEntries(List<DhcpInfo> dhcpEntries) {
			this.dhcpEntries = dhcpEntries;
		}
	}
	
	public static class RemoveDhcpEntryRsp extends AgentResponse {
	}
	
	public static class AddDhcpEntryCmd extends AgentCommand {
		private List<DhcpInfo> dhcpEntries;
		private boolean rebuild;

		public List<DhcpInfo> getDhcpEntries() {
			if (dhcpEntries == null) {
				dhcpEntries = new ArrayList<DhcpInfo>();
			}
			return dhcpEntries;
		}

		public void setDhcpEntries(List<DhcpInfo> dhcpEntries) {
			this.dhcpEntries = dhcpEntries;
		}

		public boolean isRebuild() {
			return rebuild;
		}

		public void setRebuild(boolean rebuild) {
			this.rebuild = rebuild;
		}
	}
	
	public static class AddDhcpEntryRsp extends AgentResponse {
	}

	public static class RefreshDHCPServerCmd extends AgentCommand {
		private List<DhcpServerInfo> dhcpServers;

		public List<DhcpServerInfo> getDhcpServers() {
			return dhcpServers;
		}

		public void setDhcpServers(List<DhcpServerInfo> dhcpServers) {
			this.dhcpServers = dhcpServers;
		}
	}

	public static class RefreshDHCPServerRsp extends AgentResponse {
	}
	
	public static class SNATInfo {
		private String publicNicMac;
		private String publicIp;
		private String privateNicMac;
		private String privateNicIp;
		private String privateGatewayIp;
		private String snatNetmask;
		private Boolean state;

		public String getPrivateNicMac() {
			return privateNicMac;
		}
		public void setPrivateNicMac(String nicMac) {
			this.privateNicMac = nicMac;
		}
		public String getPrivateNicIp() {
			return privateNicIp;
		}
		public void setPrivateNicIp(String nicIp) {
			this.privateNicIp = nicIp;
		}
		public String getSnatNetmask() {
			return snatNetmask;
		}
		public void setSnatNetmask(String snatNetmask) {
			this.snatNetmask = snatNetmask;
		}
		public String getPublicNicMac() {
			return publicNicMac;
		}
		public void setPublicNicMac(String publicNicMac) {
			this.publicNicMac = publicNicMac;
		}
		public String getPublicIp() {
			return publicIp;
		}
		public void setPublicIp(String publicIp) {
			this.publicIp = publicIp;
		}

		public Boolean getState() {
			return state;
		}

		public void setState(Boolean state) {
			this.state = state;
		}

		public String getPrivateGatewayIp() {
			return privateGatewayIp;
		}

		public void setPrivateGatewayIp(String privateGatewayIp) {
			this.privateGatewayIp = privateGatewayIp;
		}
	}

    public static class SyncSNATCmd extends AgentCommand {
        private List<SNATInfo> snats;
        private Boolean enable;

        public List<SNATInfo> getSnats() {
            return snats;
        }

        public void setSnats(List<SNATInfo> snats) {
            this.snats = snats;
        }

		public Boolean getEnable() {
			return enable;
		}

		public void setEnable(Boolean enable) {
			this.enable = enable;
		}
	}
    public static class SyncSNATRsp extends AgentResponse {
    }
	
	public static class SetSNATCmd extends AgentCommand {
        private SNATInfo snat;

        public SNATInfo getSnat() {
            return snat;
        }

        public void setSnat(SNATInfo snat) {
            this.snat = snat;
        }
	}
	
	public static class SetSNATRsp extends AgentResponse {
	}

    public static class RemoveSNATCmd extends AgentCommand {
        private List<SNATInfo> natInfo;

        public List<SNATInfo> getNatInfo() {
            return natInfo;
        }

        public void setNatInfo(List<SNATInfo> natInfo) {
            this.natInfo = natInfo;
        }
    }

    public static class RemoveSNATRsp extends AgentResponse {
    }
	
	public static class SyncPortForwardingRuleCmd extends AgentCommand {
	    private List<PortForwardingRuleTO> rules;

        public List<PortForwardingRuleTO> getRules() {
            return rules;
        }

        public void setRules(List<PortForwardingRuleTO> rules) {
            this.rules = rules;
        }
	}
	public static class SyncPortForwardingRuleRsp extends AgentResponse {
	}

	public static class CreatePortForwardingRuleCmd extends AgentCommand {
	    private List<PortForwardingRuleTO> rules;

        public List<PortForwardingRuleTO> getRules() {
            return rules;
        }

        public void setRules(List<PortForwardingRuleTO> rules) {
            this.rules = rules;
        }
	}
	public static class CreatePortForwardingRuleRsp extends AgentResponse {
	}
	
	public static class RevokePortForwardingRuleCmd extends AgentCommand {
	    private List<PortForwardingRuleTO> rules;

        public List<PortForwardingRuleTO> getRules() {
            return rules;
        }

        public void setRules(List<PortForwardingRuleTO> rules) {
            this.rules = rules;
        }
	}
	public static class RevokePortForwardingRuleRsp extends AgentResponse {
	}
	
	public static class DnsInfo {
		private String dnsAddress;
		private String nicMac;

		public String getNicMac() {
			return nicMac;
		}

		public void setNicMac(String nicMac) {
			this.nicMac = nicMac;
		}

		public String getDnsAddress() {
			return dnsAddress;
		}

		public void setDnsAddress(String dnsAddress) {
			this.dnsAddress = dnsAddress;
		}
	}
	
	public static class SetDnsCmd extends AgentCommand {
		private List<DnsInfo> dns;

		public List<DnsInfo> getDns() {
			return dns;
		}

		public void setDns(List<DnsInfo> dns) {
			this.dns = dns;
		}
	}
	
	public static class SetDnsRsp extends AgentResponse {
	}

	public static class SetForwardDnsCmd extends AgentCommand {
		private String dns;
        private String mac;
        private String bridgeName;
        private String nameSpace;
        private List<String> wrongDns;

        public String getNameSpace() {
            return nameSpace;
        }

        public void setNameSpace(String nameSpace) {
            this.nameSpace = nameSpace;
        }

        public String getBridgeName() {
            return bridgeName;
        }

        public void setBridgeName(String bridgeName) {
            this.bridgeName = bridgeName;
        }

        public String getDns() {
			return dns;
		}

		public void setDns(String dns) {
			this.dns = dns;
		}

        public String getMac() {
            return mac;
        }

        public void setMac(String mac) {
            this.mac = mac;
        }

        public List<String> getWrongDns() {
            return wrongDns;
        }

        public void setWrongDns(List<String> wrongDns) {
            this.wrongDns = wrongDns;
        }
    }

	public static class SetForwardDnsRsp extends AgentResponse {
	}

	public static class RemoveForwardDnsCmd extends AgentCommand {
		private String mac;
		private String bridgeName;
		private String nameSpace;

		public String getNameSpace() {
			return nameSpace;
		}

		public void setNameSpace(String nameSpace) {
			this.nameSpace = nameSpace;
		}

		public String getBridgeName() {
			return bridgeName;
		}

		public void setBridgeName(String bridgeName) {
			this.bridgeName = bridgeName;
		}

		public String getMac() {
			return mac;
		}

		public void setMac(String mac) {
			this.mac = mac;
		}
	}

	public static class RemoveForwardDnsRsp extends AgentResponse {

	}

    public static class RemoveDnsCmd extends AgentCommand {
        private List<DnsInfo> dns;

        public List<DnsInfo> getDns() {
            return dns;
        }

        public void setDns(List<DnsInfo> dns) {
            this.dns = dns;
        }
    }

    public static class RemoveDnsRsp extends AgentResponse {
    }
	
	public static class CreateVipRsp extends AgentResponse {
	}

    public static class VipTO {
        private String ip;
        private String netmask;
        private String gateway;
        private String ownerEthernetMac;
        private String vipUuid;
        private boolean isSystem;

        
        public static VipTO valueOf(VipInventory inv, String ownerMac) {
            VipTO to = new VipTO();
            to.setIp(inv.getIp());
            to.setNetmask(inv.getNetmask());
            to.setGateway(inv.getGateway());
            to.setOwnerEthernetMac(ownerMac);
            to.setVipUuid(inv.getUuid());
            to.setSystem(inv.isSystem());
            return to;
        }
        
        @Override
        public String toString() {
            return JSONObjectUtil.toJsonString(this);
        }

        public String getIp() {
            return ip;
        }

        public void setIp(String ip) {
            this.ip = ip;
        }

        public String getNetmask() {
            return netmask;
        }

        public void setNetmask(String netmask) {
            this.netmask = netmask;
        }

        public String getGateway() {
            return gateway;
        }

        public void setGateway(String gateway) {
            this.gateway = gateway;
        }

        public String getOwnerEthernetMac() {
            return ownerEthernetMac;
        }

        public void setOwnerEthernetMac(String ownerEthernetMac) {
            this.ownerEthernetMac = ownerEthernetMac;
        }

		public String getVipUuid() {
			return vipUuid;
		}

		public void setVipUuid(String vipUuid) {
			this.vipUuid = vipUuid;
		}

		public boolean isSystem() {
			return isSystem;
		}

		public void setSystem(boolean system) {
			isSystem = system;
		}
	}

	public static class NicIpTO {
		private String ip;
		private String netmask;
		private String ownerEthernetMac;


		public static NicIpTO valueOf(VmNicInventory nic) {
			NicIpTO to = new NicIpTO();
			to.setIp(nic.getIp());
			to.setNetmask(nic.getNetmask());
			to.setOwnerEthernetMac(nic.getMac());
			return to;
		}

		@Override
		public String toString() {
			return JSONObjectUtil.toJsonString(this);
		}

		public String getIp() {
			return ip;
		}

		public void setIp(String ip) {
			this.ip = ip;
		}

		public String getNetmask() {
			return netmask;
		}

		public void setNetmask(String netmask) {
			this.netmask = netmask;
		}

		public String getOwnerEthernetMac() {
			return ownerEthernetMac;
		}

		public void setOwnerEthernetMac(String ownerEthernetMac) {
			this.ownerEthernetMac = ownerEthernetMac;
		}
	}

	public static class CreateVipCmd extends AgentCommand {
		private Boolean syncVip;
	    private List<VipTO> vips;
	    /* sync all vips to virtual router together with nic ips*/
	    private List<NicIpTO> nicIps;

		public Boolean getSyncVip() {
			return syncVip;
		}

		public void setSyncVip(Boolean syncVip) {
			this.syncVip = syncVip;
		}

		public List<VipTO> getVips() {
            return vips;
        }

        public void setVips(List<VipTO> vips) {
            this.vips = vips;
        }

		public List<NicIpTO> getNicIps() {
			return nicIps;
		}

		public void setNicIps(List<NicIpTO> nicIps) {
			this.nicIps = nicIps;
		}
	}
	
	public static class RemoveVipCmd extends AgentCommand {
	    private List<VipTO> vips;

        public List<VipTO> getVips() {
            return vips;
        }

        public void setVips(List<VipTO> vips) {
            this.vips = vips;
        }
	    
	}

	public static class RemoveVipRsp extends AgentResponse {
	}

    public static class CreateEipCmd extends AgentCommand {
        private EipTO eip;

        public EipTO getEip() {
            return eip;
        }

        public void setEip(EipTO eip) {
            this.eip = eip;
        }
    }

    public static class CreateEipRsp extends AgentResponse {
    }

    public static class RemoveEipCmd extends AgentCommand {
        private EipTO eip;

        public EipTO getEip() {
            return eip;
        }

        public void setEip(EipTO eip) {
            this.eip = eip;
        }
    }

    public static class RemoveEipRsp extends AgentResponse {
    }

    public static class SyncEipCmd extends AgentCommand {
        private List<EipTO> eips;

        public List<EipTO> getEips() {
            return eips;
        }

        public void setEips(List<EipTO> eips) {
            this.eips = eips;
        }
    }

    public static class SyncEipRsp extends AgentResponse {
    }

	public static class PingCmd extends AgentCommand {
		private String uuid;

		public String getUuid() {
			return uuid;
		}

		public void setUuid(String uuid) {
			this.uuid = uuid;
		}
	}

	public static class PingRsp extends AgentResponse {
		private String uuid;
		private String version;  /* zvr version */
		private String haStatus;
		private Boolean healthy;
		private String healthDetail;
		private HashMap<String, HashMap<String, String>> serviceHealthList;

		public String getUuid() {
			return uuid;
		}

		public void setUuid(String uuid) {
			this.uuid = uuid;
		}

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

		public String getHaStatus() {
			return haStatus;
		}

		public void setHaStatus(String haStatus) {
			this.haStatus = haStatus;
		}

		public Boolean getHealthy() {
			return healthy;
		}

		public void setHealthy(Boolean healthy) {
			this.healthy = healthy;
		}

		public String getHealthDetail() {
			return healthDetail;
		}

		public void setHealthDetail(String healthDetail) {
			this.healthDetail = healthDetail;
		}

		public HashMap<String, HashMap<String, String>> getServiceHealthList() {
			return serviceHealthList;
		}

		public void setServiceHealthList(HashMap<String, HashMap<String, String>> serviceHealthList) {
			this.serviceHealthList = serviceHealthList;
		}
	}

	public static class ChangeDefaultNicCmd extends AgentCommand {
		private NicInfo newNic;
		private List<SNATInfo> snats;

		public NicInfo getNewNic() {
			return newNic;
		}

		public void setNewNic(NicInfo newNic) {
			this.newNic = newNic;
		}

		public List<SNATInfo> getSnats() {
			return snats;
		}

		public void setSnats(List<SNATInfo> snats) {
			this.snats = snats;
		}
	}

	public static class ChangeDefaultNicRsp extends AgentResponse {

	}

	public static class ConfigureNtpCmd extends AgentCommand {
		private List<String> timeServers;

		public List<String> getTimeServers() {
			return timeServers;
		}

		public void setTimeServers(List<String> timeServers) {
			this.timeServers = timeServers;
		}

	}

	public static class ConfigureNtpRsp extends AgentResponse {

	}
}
