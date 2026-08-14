package org.zstack.simulator.virtualrouter;

import org.zstack.network.service.virtualrouter.VirtualRouterCommands.*;
import org.zstack.network.service.virtualrouter.eip.EipTO;
import org.zstack.network.service.virtualrouter.lb.VirtualRouterLoadBalancerBackend;
import org.zstack.network.service.virtualrouter.lb.VirtualRouterLoadBalancerBackend.DeleteLbCmd;
import org.zstack.network.service.virtualrouter.lb.VirtualRouterLoadBalancerBackend.RefreshLbCmd;
import org.zstack.network.service.virtualrouter.lb.VirtualRouterLoadBalancerBackend.CertificateCmd;
import org.zstack.network.service.virtualrouter.lb.VirtualRouterLoadBalancerBackend.CertificateRsp;
import org.zstack.network.service.virtualrouter.portforwarding.PortForwardingRuleTO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VirtualRouterSimulatorConfig {
	public volatile boolean setDhcpEntrySuccess = true;
	public volatile boolean configureNicSuccess = true;
	public volatile boolean setSNATSuccess = true;
	public volatile boolean setDnsSuccess = true;
	public volatile boolean removedDhcpSuccess = true;
	public volatile boolean portForwardingSuccess = true;
	public volatile boolean vipSuccess = true;
    public volatile boolean eipSuccess = true;
    public volatile boolean removeEipSuccess = true;
    public volatile boolean syncEipSuccess = true;
    public volatile List<EipTO> eips = new ArrayList<EipTO>();
    public volatile List<InitCommand> initCommands = new ArrayList<InitCommand>();
    public volatile List<EipTO> removedEips = new ArrayList<EipTO>();
	public volatile List<DhcpInfo> dhcpInfos = new ArrayList<DhcpInfo>();
    public volatile Map<String, DhcpInfo> dhcpInfoMap = new HashMap<String, DhcpInfo>();
	public volatile List<SNATInfo> snatInfos = new ArrayList<SNATInfo>();
	public volatile List<DnsInfo> dnsInfo = new ArrayList<DnsInfo>();
	public volatile List<DhcpInfo> removedDhcp = new ArrayList<DhcpInfo>();
	public volatile List<PortForwardingRuleTO> portForwardingRules = new ArrayList<PortForwardingRuleTO>();
	public volatile List<PortForwardingRuleTO> removedPortForwardingRules = new ArrayList<PortForwardingRuleTO>();
	public volatile List<VipTO> vips = new ArrayList<VipTO>();
	public volatile List<VipTO> removedVips = new ArrayList<VipTO>();
	public volatile List<PingCmd> pingCmds = new ArrayList<PingCmd>();
	public volatile String uuid;
	public volatile List<RefreshLbCmd> refreshLbCmds = new ArrayList<RefreshLbCmd>();
    public volatile boolean refreshLbSuccess = true;
	public volatile List<DeleteLbCmd> deleteLbCmds = new ArrayList<DeleteLbCmd>();
	public volatile List<RemoveDnsCmd> removeDnsCmds = new ArrayList<RemoveDnsCmd>();
	public volatile CertificateCmd certificateCmd;
	public volatile CertificateRsp certificateRsp;

}
