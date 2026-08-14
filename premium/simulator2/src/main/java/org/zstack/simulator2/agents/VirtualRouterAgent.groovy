package org.zstack.simulator2.agents

import org.springframework.http.HttpEntity
import org.zstack.appliancevm.ApplianceVmCommands
import org.zstack.appliancevm.ApplianceVmConstant
import org.zstack.appliancevm.ApplianceVmKvmCommands
import org.zstack.ipsec.vyos.VyosIPsecBackend
import org.zstack.network.service.vipQos.vyos.VyosVipQosBackend
import org.zstack.network.service.virtualrouter.VirtualRouterCommands
import org.zstack.network.service.virtualrouter.VirtualRouterConstant
import org.zstack.network.service.virtualrouter.VirtualRouterGlobalProperty
import org.zstack.network.service.virtualrouter.dns.VirtualRouterCentralizedDnsBackend
import org.zstack.network.service.virtualrouter.lb.VirtualRouterLoadBalancerBackend
import org.zstack.routeProtocol.RouteProtocolOspfBackend
import org.zstack.simulator2.Simulator
import org.zstack.simulator2.SimulatorGlobalProperty
import org.zstack.simulator2.config.primaryStorage.AgentProperty
import org.zstack.vpc.VpcManagerImpl
import org.zstack.header.vpc.VpcConstants
import org.zstack.vpc.ha.vyos.VpcHaRouterCommands

/**
 * Created by xing5 on 2017/9/19.
 */
class VirtualRouterAgent extends Agent {
    VirtualRouterAgent(Simulator simulator) {
        super(simulator)

        VirtualRouterGlobalProperty.AGENT_PORT = SimulatorGlobalProperty.SIMULATOR_AGENT_PORT
    }

    @Override
    void setupAgentHandler() {
        handle(ApplianceVmConstant.INIT_PATH) {
            return new ApplianceVmCommands.InitRsp()
        }

        handle(ApplianceVmConstant.REFRESH_FIREWALL_PATH) {
            return new ApplianceVmCommands.RefreshFirewallRsp()
        }

        handle(ApplianceVmKvmCommands.PrepareBootstrapInfoCmd.PATH) {
            return new ApplianceVmKvmCommands.PrepareBootstrapInfoRsp()
        }

        handle(ApplianceVmConstant.ECHO_PATH) { HttpEntity<String> e ->
            return [:]
        }

        handle(VirtualRouterConstant.VR_INIT) {
            return new VirtualRouterCommands.InitRsp()
        }

        handle(VirtualRouterConstant.VR_ADD_DHCP_PATH) {
            return new VirtualRouterCommands.AddDhcpEntryRsp()
        }

        handle(VirtualRouterConstant.VR_REFRESH_DHCP_SERVER_PATH) {
            return new VirtualRouterCommands.RefreshDHCPServerRsp()
        }

        handle(VirtualRouterConstant.VR_START_DHCP_SERVER_PATH) {
            return new VirtualRouterCommands.RefreshDHCPServerRsp()
        }

        handle(VirtualRouterConstant.VR_STOP_DHCP_SERVER_PATH) {
            return new VirtualRouterCommands.RefreshDHCPServerRsp()
        }

        handle(VirtualRouterConstant.VR_REVOKE_PORT_FORWARDING) {
            return new VirtualRouterCommands.RevokePortForwardingRuleRsp()
        }

        handle(VirtualRouterConstant.VR_CREATE_EIP) {
            return new VirtualRouterCommands.CreateEipRsp()
        }

        handle(VirtualRouterConstant.VR_REMOVE_EIP) {
            return new VirtualRouterCommands.RemoveEipRsp()
        }

        handle(VirtualRouterConstant.VR_SYNC_EIP) {
            return new VirtualRouterCommands.SyncEipRsp()
        }

        handle(VirtualRouterConstant.VR_CREATE_VIP) {
            return new VirtualRouterCommands.CreateVipRsp()
        }

        handle(VirtualRouterConstant.VR_REMOVE_VIP) {
            return new VirtualRouterCommands.RemoveVipRsp()
        }

        handle(VirtualRouterConstant.VR_SYNC_PORT_FORWARDING) {
            return new VirtualRouterCommands.SyncPortForwardingRuleRsp()
        }

        handle(VirtualRouterConstant.VR_CREATE_PORT_FORWARDING) {
            return new VirtualRouterCommands.CreatePortForwardingRuleRsp()
        }

        handle(VirtualRouterConstant.VR_ECHO_PATH) { HttpEntity<String> e ->
            return [:]
        }

        handle(VirtualRouterConstant.VR_PING) { HttpEntity<String> e ->
            VirtualRouterCommands.PingCmd cmd = json(e, VirtualRouterCommands.PingCmd.class)
            VirtualRouterCommands.PingRsp rsp = new VirtualRouterCommands.PingRsp()
            rsp.uuid = cmd.uuid
            rsp.version = "3.10.0.0"
            return rsp
        }

        handle(VirtualRouterConstant.VR_SYNC_SNAT_PATH) {
            return new VirtualRouterCommands.SyncSNATRsp()
        }

        handle(VirtualRouterLoadBalancerBackend.REFRESH_LB_PATH) {
            return new VirtualRouterLoadBalancerBackend.RefreshLbRsp()
        }

        handle(VirtualRouterLoadBalancerBackend.REFRESH_LB_LOG_LEVEL_PATH) {
            return new VirtualRouterLoadBalancerBackend.RefreshLbLogLevelRsp()
        }

        handle(VirtualRouterLoadBalancerBackend.CREATE_CERTIFICATE_PATH) {
            return new VirtualRouterLoadBalancerBackend.RefreshLbRsp()
        }

        handle(VirtualRouterLoadBalancerBackend.DELETE_CERTIFICATE_PATH) {
            return new VirtualRouterLoadBalancerBackend.RefreshLbRsp()
        }

        handle(VirtualRouterLoadBalancerBackend.DELETE_LB_PATH) {
            return new VirtualRouterLoadBalancerBackend.DeleteLbRsp()
        }

        handle(VirtualRouterConstant.VR_SET_SNAT_PATH) {
            return new VirtualRouterCommands.SetSNATRsp()
        }

        handle(VirtualRouterConstant.VR_REMOVE_SNAT_PATH) {
            return new VirtualRouterCommands.RemoveSNATRsp()
        }

        handle(VirtualRouterCentralizedDnsBackend.SET_DNS_FORWARD_PATH) {
            return new VirtualRouterCommands.SetForwardDnsRsp()
        }

        handle(VirtualRouterCentralizedDnsBackend.REMOVE_DNS_FORWARD_PATH) {
            return new VirtualRouterCommands.RemoveForwardDnsRsp()
        }

        handle(VirtualRouterConstant.VR_REMOVE_DNS_PATH) {
            return new VirtualRouterCommands.RemoveDnsRsp()
        }

        handle(VirtualRouterConstant.VR_SET_DNS_PATH) {
            return new VirtualRouterCommands.SetDnsRsp()
        }

        handle(VirtualRouterConstant.VR_CONFIGURE_NIC_PATH) {
            return new VirtualRouterCommands.ConfigureNicRsp()
        }

        handle(VirtualRouterConstant.VR_REMOVE_NIC_PATH) {
            return new VirtualRouterCommands.RemoveNicRsp()
        }

        handle(VirtualRouterConstant.VR_REMOVE_DHCP_PATH) {
            return new VirtualRouterCommands.RemoveDhcpEntryRsp()
        }

        handle(VyosIPsecBackend.CREATE_IPSEC_CONNECTION) {
            return new VyosIPsecBackend.CreateIPsecConnectionRsp()
        }

        handle(VyosIPsecBackend.SYNC_IPSEC_CONNECTION) {
            return new VyosIPsecBackend.SyncIPsecConnectionRsp()
        }

        handle(VyosIPsecBackend.DELETE_IPSEC_CONNECTION) {
            return new VyosIPsecBackend.DeleteIPsecConnectionRsp()
        }

        handle(VyosVipQosBackend.VR_SET_VIP_QOS) {
            return new VyosVipQosBackend.SetVipQosRsp()
        }

        handle(VyosVipQosBackend.VR_DELETE_VIP_QOS) {
            return new VyosVipQosBackend.DeleteVipQosRsp()
        }

        handle(VyosVipQosBackend.VR_DELETE_VIPALL_QOS) {
            return new VyosVipQosBackend.DeleteVipAllQosRsp()
        }

        handle(VirtualRouterConstant.VR_CONFIGURE_NIC_FIREWALL_DEFAULT_ACTION_PATH) {
            return new VirtualRouterCommands.ConfigureNicFirewallDefaultActionRsp()
        }

        handle(VpcConstants.VR_SET_VPCDNS_PATH) {
            return new org.zstack.vpc.VpcRouterCommands.VpcRouterSetDnsRsp()
        }

        handle(VpcManagerImpl.ZSN_STATUS_PATH) {
            def ret = simulator.sqlite.find("select * from ${AgentProperty.class.simpleName} where name = 'distributedRouting'", AgentProperty.class)

            if (ret == null) {
                ret = true
            }

            VpcManagerImpl.GetStatusRsp rsp = new VpcManagerImpl.GetStatusRsp()
            rsp.rawStatus = "{\"distributedRouting\":\"${ret}\",\"tmout\":\"10s\"}"
            rsp.setSuccess(true)

            return rsp
        }

        handle(VpcConstants.VR_SET_VPC_NETWORK_SERVICE_SNAT_STATE_PATH) { HttpEntity<String> e ->
            VpcManagerImpl.SetNetworkServiceSnatCmd cmd = json(e, VpcManagerImpl.SetNetworkServiceSnatCmd.class)
            VpcManagerImpl.SetNetworkServiceRsp rsp = new VpcManagerImpl.SetNetworkServiceRsp()
            if (cmd.enabled) {
                rsp.serviceStatus = "enabled"
            } else {
                rsp.serviceStatus = "disabled"
            }
            return rsp
        }

        handle(RouteProtocolOspfBackend.ROUTER_PROTOCOL_REFRESH_OSPF) {
            return new RouteProtocolOspfBackend.SetOspfRsp()
        }

        handle(RouteProtocolOspfBackend.ROUTER_PROTOCOL_GET_OSPF_NEIGHBOR) {
            RouteProtocolOspfBackend.GetOspfNeighborRsp rsp = new RouteProtocolOspfBackend.GetOspfNeighborRsp()
            rsp.neighbors = []
            return rsp
        }

        handle(VpcHaRouterCommands.VYOS_HA_ENABLE_PATH) {
            return new VpcHaRouterCommands.VyosHaEnableRsp()
        }

        handle(VpcHaRouterCommands.SYNC_VPC_ROUTER_HA_PATH) {
            return new VpcHaRouterCommands.SyncVpcRouterHaRsp()
        }

        handle(VpcHaRouterCommands.RESTART_KEEPALIVED_PATH) {
            return new VpcHaRouterCommands.RestartKeepalivedRsp()
        }

        handle(VirtualRouterConstant.VR_CHANGE_DEFAULT_ROUTE_NETWORK) {
            return new VirtualRouterCommands.ChangeDefaultNicRsp()
        }
    }
}