package org.zstack.network.service.virtualrouter;

import org.zstack.kvm.KVMAgentCommands;

import java.util.List;

public class VirtualRouterKvmBackendCommands {
	
	public static class BootstrapIsoInfo {
		private String managementNicIp;
		private String managementNicNetmask;
		private String managementNicGateway;
		private String managementNicMac;
		private String publicNicIp;
		private String publicNicNetmask;
		private String publicNicGateway;
		private String publicNicMac;
		private List<String> dns;
		public String getManagementNicIp() {
			return managementNicIp;
		}
		public void setManagementNicIp(String managementNicIp) {
			this.managementNicIp = managementNicIp;
		}
		public String getManagementNicNetmask() {
			return managementNicNetmask;
		}
		public void setManagementNicNetmask(String managementNicNetmask) {
			this.managementNicNetmask = managementNicNetmask;
		}
		public String getManagementNicGateway() {
			return managementNicGateway;
		}
		public void setManagementNicGateway(String managementNicGateway) {
			this.managementNicGateway = managementNicGateway;
		}
		public String getManagementNicMac() {
			return managementNicMac;
		}
		public void setManagementNicMac(String managementNicMac) {
			this.managementNicMac = managementNicMac;
		}
		public String getPublicNicIp() {
			return publicNicIp;
		}
		public void setPublicNicIp(String publicNicIp) {
			this.publicNicIp = publicNicIp;
		}
		public String getPublicNicNetmask() {
			return publicNicNetmask;
		}
		public void setPublicNicNetmask(String publicNicNetmask) {
			this.publicNicNetmask = publicNicNetmask;
		}
		public String getPublicNicGateway() {
			return publicNicGateway;
		}
		public void setPublicNicGateway(String publicNicGateway) {
			this.publicNicGateway = publicNicGateway;
		}
		public String getPublicNicMac() {
			return publicNicMac;
		}
		public void setPublicNicMac(String publicNicMac) {
			this.publicNicMac = publicNicMac;
		}
		public List<String> getDns() {
			return dns;
		}
		public void setDns(List<String> dns) {
			this.dns = dns;
		}
	}
	
	public static class CreateVritualRouterBootstrapIsoCmd extends KVMAgentCommands.AgentCommand {
		private BootstrapIsoInfo isoInfo;
		private String isoPath;
		public BootstrapIsoInfo getIsoInfo() {
			return isoInfo;
		}
		public void setIsoInfo(BootstrapIsoInfo isoInfo) {
			this.isoInfo = isoInfo;
		}
		public String getIsoPath() {
			return isoPath;
		}
		public void setIsoPath(String isoPath) {
			this.isoPath = isoPath;
		}
	}
	
	public static class CreateVritualRouterBootstrapIsoRsp extends KVMAgentCommands.AgentResponse {
	}
	
	public static class DeleteVirtualRouterBootstrapIsoCmd extends KVMAgentCommands.AgentCommand {
		public String isoPath;

		public String getIsoPath() {
			return isoPath;
		}

		public void setIsoPath(String isoPath) {
			this.isoPath = isoPath;
		}
	}
	
	public static class DeleteVirtualRouterBootstrapIsoRsp extends KVMAgentCommands.AgentResponse {
	}
}
