package org.zstack.testlib

import org.springframework.http.HttpEntity
import org.zstack.appliancevm.ApplianceVmCommands
import org.zstack.appliancevm.ApplianceVmConstant
import org.zstack.appliancevm.ApplianceVmKvmCommands
import org.zstack.network.service.virtualrouter.VirtualRouterCommands
import org.zstack.network.service.virtualrouter.VirtualRouterConstant
import org.zstack.network.service.virtualrouter.dns.VirtualRouterCentralizedDnsBackend
import org.zstack.network.service.virtualrouter.lb.VirtualRouterLoadBalancerBackend
import org.zstack.utils.gson.JSONObjectUtil

/**
 * Created by xing5 on 2017/2/15.
 */
class VirtualRouterOfferingSpec extends InstanceOfferingSpec {
    private Closure managementL3Network
    private Closure publicL3Network
    private Closure image
    @SpecParam
    Boolean isDefault

    VirtualRouterOfferingSpec(EnvSpec envSpec) {
        super(envSpec)
    }

    class Simulators implements Simulator {
        @Override
        void registerSimulators(EnvSpec xspec) {
            def simulator = { arg1, arg2 ->
                xspec.simulator(arg1, arg2)
            }

            simulator(ApplianceVmConstant.INIT_PATH) {
                return new ApplianceVmCommands.InitRsp()
            }

            simulator(ApplianceVmConstant.REFRESH_FIREWALL_PATH) {
                return new ApplianceVmCommands.RefreshFirewallRsp()
            }

            simulator(ApplianceVmKvmCommands.PrepareBootstrapInfoCmd.PATH) {
                return new ApplianceVmKvmCommands.PrepareBootstrapInfoRsp()
            }

            simulator(ApplianceVmConstant.ECHO_PATH) { HttpEntity<String> e ->
                Spec.checkHttpCallType(e, true)
                return [:]
            }

            simulator(VirtualRouterConstant.VR_INIT) {
                return new VirtualRouterCommands.InitRsp()
            }

            simulator(VirtualRouterConstant.VR_ADD_DHCP_PATH) {
                return new VirtualRouterCommands.AddDhcpEntryRsp()
            }

            simulator(VirtualRouterConstant.VR_REFRESH_DHCP_SERVER_PATH) {
                return new VirtualRouterCommands.RefreshDHCPServerRsp()
            }

            simulator(VirtualRouterConstant.VR_START_DHCP_SERVER_PATH) {
                return new VirtualRouterCommands.RefreshDHCPServerRsp()
            }

            simulator(VirtualRouterConstant.VR_STOP_DHCP_SERVER_PATH) {
                return new VirtualRouterCommands.RefreshDHCPServerRsp()
            }

            simulator(VirtualRouterConstant.VR_REVOKE_PORT_FORWARDING) {
                return new VirtualRouterCommands.RevokePortForwardingRuleRsp()
            }

            simulator(VirtualRouterConstant.VR_CREATE_EIP) {
                return new VirtualRouterCommands.CreateEipRsp()
            }

            simulator(VirtualRouterConstant.VR_REMOVE_EIP) {
                return new VirtualRouterCommands.RemoveEipRsp()
            }

            simulator(VirtualRouterConstant.VR_SYNC_EIP) {
                return new VirtualRouterCommands.SyncEipRsp()
            }

            simulator(VirtualRouterConstant.VR_CREATE_VIP) {
                return new VirtualRouterCommands.CreateVipRsp()
            }

            simulator(VirtualRouterConstant.VR_REMOVE_VIP) {
                return new VirtualRouterCommands.RemoveVipRsp()
            }

            simulator(VirtualRouterConstant.VR_SYNC_PORT_FORWARDING) {
                return new VirtualRouterCommands.SyncPortForwardingRuleRsp()
            }

            simulator(VirtualRouterConstant.VR_CREATE_PORT_FORWARDING) {
                return new VirtualRouterCommands.CreatePortForwardingRuleRsp()
            }

            simulator(VirtualRouterConstant.VR_ECHO_PATH) { HttpEntity<String> e ->
                Spec.checkHttpCallType(e, true)
                return [:]
            }

            simulator(VirtualRouterConstant.VR_PING) { HttpEntity<String> e ->
                VirtualRouterCommands.PingCmd cmd = JSONObjectUtil.toObject(e.body, VirtualRouterCommands.PingCmd.class)
                VirtualRouterCommands.PingRsp rsp = new VirtualRouterCommands.PingRsp()
                rsp.uuid = cmd.uuid
                rsp.version = "3.10.0.0"
                return rsp
            }

            simulator(VirtualRouterConstant.VR_SYNC_SNAT_PATH) {
                return new VirtualRouterCommands.SyncSNATRsp()
            }

            simulator(VirtualRouterLoadBalancerBackend.REFRESH_LB_PATH) {
                return new VirtualRouterLoadBalancerBackend.RefreshLbRsp()
            }

            simulator(VirtualRouterLoadBalancerBackend.DELETE_LB_PATH) {
                return new VirtualRouterLoadBalancerBackend.DeleteLbRsp()
            }

            simulator(VirtualRouterLoadBalancerBackend.CREATE_CERTIFICATE_PATH) {
                return new VirtualRouterLoadBalancerBackend.CertificateRsp()
            }

            simulator(VirtualRouterLoadBalancerBackend.DELETE_CERTIFICATE_PATH) {
                return new VirtualRouterLoadBalancerBackend.CertificateRsp()
            }

            simulator(VirtualRouterConstant.VR_SET_SNAT_PATH) {
                return new VirtualRouterCommands.SetSNATRsp()
            }

            simulator(VirtualRouterCentralizedDnsBackend.SET_DNS_FORWARD_PATH) {
                return new VirtualRouterCommands.SetForwardDnsRsp()
            }

            simulator(VirtualRouterCentralizedDnsBackend.REMOVE_DNS_FORWARD_PATH) {
                return new VirtualRouterCommands.RemoveForwardDnsRsp()
            }

            simulator(VirtualRouterConstant.VR_REMOVE_DNS_PATH) {
                return new VirtualRouterCommands.RemoveDnsRsp()
            }

            simulator(VirtualRouterConstant.VR_SET_DNS_PATH) {
                return new VirtualRouterCommands.SetDnsRsp()
            }

            simulator(VirtualRouterConstant.VR_CONFIGURE_NIC_PATH) {
                return new VirtualRouterCommands.ConfigureNicRsp()
            }

            simulator(VirtualRouterConstant.VR_CONFIGURE_NIC_FIREWALL_DEFAULT_ACTION_PATH) {
                return new VirtualRouterCommands.ConfigureNicFirewallDefaultActionRsp()
            }

            simulator(VirtualRouterConstant.VR_REMOVE_NIC_PATH) {
                return new VirtualRouterCommands.RemoveNicRsp()
            }

            simulator(VirtualRouterConstant.VR_REMOVE_DHCP_PATH) {
                return new VirtualRouterCommands.RemoveDhcpEntryRsp()
            }

            simulator(VirtualRouterConstant.VR_CHANGE_DEFAULT_ROUTE_NETWORK) {
                return new VirtualRouterCommands.ChangeDefaultNicRsp()
            }
        }
    }

    @Override
    SpecID create(String uuid, String sessionId) {
        inventory = createVirtualRouterOffering {
            delegate.resourceUuid = uuid
            delegate.name = name
            delegate.memorySize = memory
            delegate.cpuNum = cpu
            delegate.allocatorStrategy = allocatorStrategy
            delegate.userTags = userTags
            delegate.systemTags = systemTags
            delegate.managementNetworkUuid = managementL3Network()
            delegate.publicNetworkUuid = publicL3Network()
            delegate.imageUuid = image()
            delegate.zoneUuid = (parent as ZoneSpec).inventory.uuid
            delegate.isDefault = isDefault
        }

        postCreate {
            inventory = queryVirtualRouterOffering {
                conditions=["uuid=${inventory.uuid}".toString()]
            }[0]
        }

        return id(name, inventory.uuid)
    }

    private Closure l3Network(String name) {
        preCreate {
            addDependency(name, L3NetworkSpec.class)
        }

        return {
            L3NetworkSpec l3 = findSpec(name, L3NetworkSpec.class)
            assert l3 != null: "cannot find the L3 network[$name] defined in VirtualRouterOfferingSpec"
            return l3.inventory.uuid
        }
    }

    @SpecMethod
    void useManagementL3Network(String name) {
        managementL3Network = l3Network(name)
    }

    @SpecMethod
    void usePublicL3Network(String name) {
        publicL3Network = l3Network(name)
    }

    @SpecMethod
    void useImage(String name) {
        preCreate {
            addDependency(name, ImageSpec.class)
        }

        image = {
            ImageSpec i = findSpec(name, ImageSpec.class)
            assert i != null: "cannot find the image[$name] defined in VirtualRouterOfferingSpec"
            return i.inventory.uuid
        }
    }
}
