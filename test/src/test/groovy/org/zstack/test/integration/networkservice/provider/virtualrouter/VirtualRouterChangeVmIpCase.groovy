package org.zstack.test.integration.networkservice.provider.virtualrouter

import org.springframework.http.HttpEntity
import org.zstack.core.db.Q
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.network.securitygroup.SecurityGroupConstant
import org.zstack.header.vm.VmNicVO
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMSecurityGroupBackend
import org.zstack.network.securitygroup.APIAddSecurityGroupRuleMsg
import org.zstack.network.securitygroup.VmNicSecurityTO
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.virtualrouter.*
import org.zstack.network.service.lb.LoadBalancerConstants
import org.zstack.network.service.virtualrouter.lb.VirtualRouterLoadBalancerBackend
import org.zstack.network.service.portforwarding.PortForwardingConstant
import org.zstack.network.service.portforwarding.PortForwardingProtocolType
import org.zstack.network.service.portforwarding.PortForwardingRuleVO
import org.zstack.network.service.portforwarding.PortForwardingRuleVO_
import org.zstack.network.service.virtualrouter.VirtualRouterCommands;
import org.zstack.network.service.virtualrouter.VirtualRouterConstant
import org.zstack.network.service.virtualrouter.portforwarding.VirtualRouterPortForwardingRuleRefVO
import org.zstack.network.service.virtualrouter.portforwarding.VirtualRouterPortForwardingRuleRefVO_
import org.zstack.network.service.virtualrouter.vyos.VyosConstants
import org.zstack.sdk.*
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil
import org.zstack.utils.network.IPv6Constants
import static org.zstack.utils.CollectionDSL.list

class VirtualRouterChangeVmIpCase extends SubCase {
    EnvSpec env
    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(NetworkServiceProviderTest.springSpec)
    }

    @Override
    void environment() {
        env = env {
            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(1)
                cpu = 1
            }
            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                username = "root"
                password = "password"
                hostname = "localhost"

                image {
                    name = "image1"
                    url  = "http://zstack.org/download/test.qcow2"
                }
                image {
                    name = "vr"
                    url = "http://zstack.org/download/vr.qcow2"
                }
            }
            zone {
                name = "zone"
                description = "test"

                cluster {
                    name = "cluster"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm"
                        managementIp = "127.0.0.1"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("local")
                    attachL2Network("l2")
                    attachL2Network("l2-2")
                }

                localPrimaryStorage {
                    name = "local"
                    url = "/local_ps"
                }

                l2NoVlanNetwork {
                    name = "l2"
                    physicalInterface = "eth0"

                    l3Network {
                        name = "vrL3"
                        service {
                            provider = VyosConstants.VYOS_ROUTER_PROVIDER_TYPE
                            types = [NetworkServiceType.DHCP.toString(),
                                     NetworkServiceType.DNS.toString(),
                                     NetworkServiceType.SNAT.toString(),
                                     PortForwardingConstant.PORTFORWARDING_NETWORK_SERVICE_TYPE,
                                     LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE_STRING,
                                     EipConstant.EIP_NETWORK_SERVICE_TYPE]
                        }
                        service {
                            provider = SecurityGroupConstant.SECURITY_GROUP_PROVIDER_TYPE
                            types = [SecurityGroupConstant.SECURITY_GROUP_NETWORK_SERVICE_TYPE]
                        }
                        ip {
                            startIp = "192.165.100.10"
                            endIp = "192.165.100.100"
                            netmask = "255.255.255.0"
                            gateway = "192.165.100.1"
                        }
                    }

                    l3Network {
                        name = "pubL3"

                        ip {
                            startIp = "11.11.11.11"
                            endIp = "11.11.11.200"
                            netmask = "255.255.255.0"
                            gateway = "11.11.11.1"
                        }
                    }
                }

                l2VlanNetwork {
                    name = "l2-2"
                    physicalInterface = "eth1"
                    vlan = 2222

                    l3Network {
                        name = "vrL3_2"
                        service {
                            provider = VyosConstants.VYOS_ROUTER_PROVIDER_TYPE
                            types = [NetworkServiceType.DHCP.toString(),
                                     NetworkServiceType.DNS.toString(),
                                     NetworkServiceType.SNAT.toString(),
                                     PortForwardingConstant.PORTFORWARDING_NETWORK_SERVICE_TYPE,
                                     LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE_STRING,
                                     EipConstant.EIP_NETWORK_SERVICE_TYPE]
                        }
                        service {
                            provider = SecurityGroupConstant.SECURITY_GROUP_PROVIDER_TYPE
                            types = [SecurityGroupConstant.SECURITY_GROUP_NETWORK_SERVICE_TYPE]
                        }
                        ip {
                            startIp = "192.165.200.10"
                            endIp = "192.165.200.100"
                            netmask = "255.255.255.0"
                            gateway = "192.165.200.1"
                        }
                    }
                }

                attachBackupStorage("sftp")

                virtualRouterOffering {
                    name = "vro"
                    memory = SizeUnit.GIGABYTE.toByte(1)
                    cpu = 1
                    useManagementL3Network("pubL3")
                    usePublicL3Network("pubL3")
                    useImage("vr")
                }
            }
        }
    }

    @Override
    void test() {
        env.create() {
            testPFWhenChangeIp()
            testLBWhenChangeIp()
            testBaseServiceWhenChangeIp()
            testBaseServiceWhenChangeL3()
        }
    }

    void testPFWhenChangeIp() {
        L3NetworkInventory vr_l3 = env.inventoryByName("vrL3")
        L3NetworkInventory pub_l3 = env.inventoryByName("pubL3")

        VmInstanceInventory vm = createVmInstance {
            name = "vm-pf-change-ip"
            imageUuid = env.inventoryByName("image1").uuid
            instanceOfferingUuid = env.inventoryByName("instanceOffering").uuid
            l3NetworkUuids = [vr_l3.uuid]
        }
        VmNicInventory vmNic = vm.getVmNics().get(0)
        VmNicVO vmNicVO = dbFindByUuid(vmNic.uuid, VmNicVO.class)
        String ip1 = vmNicVO.getIp()

        VipInventory vip = createVip {
            name = "vip"
            l3NetworkUuid = pub_l3.uuid
        }

        PortForwardingRuleInventory portForwarding = createPortForwardingRule {
            name = "pf-test"
            vipUuid = vip.uuid
            vipPortStart = 22
            vipPortEnd = 22
            privatePortEnd = 100
            privatePortStart = 100
            protocolType = PortForwardingProtocolType.TCP.toString()
        }

        attachPortForwardingRule {
            vmNicUuid = vmNic.uuid
            ruleUuid = portForwarding.uuid
        }

        VirtualRouterCommands.CreatePortForwardingRuleCmd createPFCmd = null
        env.afterSimulator(VirtualRouterConstant.VR_CREATE_PORT_FORWARDING){rsp, HttpEntity<String> entity ->
            createPFCmd = json(entity.body,VirtualRouterCommands.CreatePortForwardingRuleCmd.class)
            return rsp
        }
        VirtualRouterCommands.RevokePortForwardingRuleCmd revokePFCmd = null
        env.afterSimulator(VirtualRouterConstant.VR_REVOKE_PORT_FORWARDING){rsp, HttpEntity<String> entity ->
            revokePFCmd = json(entity.body,VirtualRouterCommands.RevokePortForwardingRuleCmd.class)
            return rsp
        }

        List<FreeIpInventory> freeIp4s = getFreeIp {
            l3NetworkUuid = vr_l3.uuid
            ipVersion = IPv6Constants.IPv4
        }
        String ip2 = freeIp4s.get(0).getIp()
        setVmStaticIp {
            vmInstanceUuid = vm.uuid
            l3NetworkUuid = vr_l3.uuid
            ip = ip2
        }

        retryInSecs {
            assert revokePFCmd != null
            assert revokePFCmd.rules.get(0).privateIp == ip1
            assert createPFCmd != null
            assert createPFCmd.rules.get(0).privateIp == ip2
        }

        assert ip1 != ip2
    }

    void testLBWhenChangeIp() {
        L3NetworkInventory vr_l3 = env.inventoryByName("vrL3")
        L3NetworkInventory pub_l3 = env.inventoryByName("pubL3")
        VmInstanceInventory vm = createVmInstance {
            name = "vm-lb-change-ip"
            imageUuid = env.inventoryByName("image1").uuid
            instanceOfferingUuid = env.inventoryByName("instanceOffering").uuid
            l3NetworkUuids = [vr_l3.uuid]
        }
        VmNicInventory vmNic = vm.getVmNics().get(0)
        VmNicVO vmNicVO = dbFindByUuid(vmNic.uuid, VmNicVO.class)
        String ip1 = vmNicVO.getIp()

        VipInventory vip = createVip {
            name = "vip-1"
            l3NetworkUuid = pub_l3.uuid
        }

        LoadBalancerInventory lb = createLoadBalancer {
            name = "test-lb-1"
            vipUuid = vip.getUuid()
        }
        LoadBalancerListenerInventory lbl1 = createLoadBalancerListener {
            loadBalancerUuid = lb.uuid
            name = "listener"
            instancePort = 22
            loadBalancerPort = 222
            protocol = LoadBalancerConstants.LB_PROTOCOL_TCP
        }
        LoadBalancerServerGroupInventory sg1 = createLoadBalancerServerGroup{
            loadBalancerUuid = lb.uuid
            name = "lb-group-1"
        }

        addBackendServerToServerGroup {
            vmNics = [['uuid':vmNic.uuid,'weight':'20']]
            serverGroupUuid = sg1.uuid
        }

        VirtualRouterLoadBalancerBackend.RefreshLbCmd refreshLbCmd = null
        env.afterSimulator(VirtualRouterLoadBalancerBackend.REFRESH_LB_PATH) { rsp, HttpEntity<String> e ->
            refreshLbCmd = JSONObjectUtil.toObject(e.body, VirtualRouterLoadBalancerBackend.RefreshLbCmd.class)
            return rsp
        }

        assert refreshLbCmd == null

        addServerGroupToLoadBalancerListener {
            listenerUuid = lbl1.uuid
            serverGroupUuid = sg1.uuid
        }

        List<FreeIpInventory> freeIp4s = getFreeIp {
            l3NetworkUuid = vr_l3.uuid
            ipVersion = IPv6Constants.IPv4
        }
        String ip2 = freeIp4s.get(0).getIp()
        setVmStaticIp {
            vmInstanceUuid = vm.uuid
            l3NetworkUuid = vr_l3.uuid
            ip = ip2
        }

        retryInSecs {
            assert refreshLbCmd != null
            assert refreshLbCmd.lbs.size() == 1
        }

        assert ip1 != ip2
    }

    void testBaseServiceWhenChangeIp() {
        L3NetworkInventory vr_l3 = env.inventoryByName("vrL3")
        L3NetworkInventory pub_l3 = env.inventoryByName("pubL3")
        VmInstanceInventory vm = createVmInstance {
            name = "vm-dhcp-change-ip"
            imageUuid = env.inventoryByName("image1").uuid
            instanceOfferingUuid = env.inventoryByName("instanceOffering").uuid
            l3NetworkUuids = [vr_l3.uuid]
        }
        VmNicInventory vmNic = vm.getVmNics().get(0)
        VmNicVO vmNicVO = dbFindByUuid(vmNic.uuid, VmNicVO.class)
        String ip1 = vmNicVO.getIp()

        def sg = createSecurityGroup {
            name = "sg-1"
            ipVersion = 4
        } as SecurityGroupInventory
        attachSecurityGroupToL3Network {
            securityGroupUuid = sg.uuid
            l3NetworkUuid = vr_l3.uuid
        }
        addVmNicToSecurityGroup {
            securityGroupUuid = sg.uuid
            vmNicUuids = [vmNic.uuid]
        }

        def vip = createVip {
            name = "vip"
            l3NetworkUuid = pub_l3.uuid
        } as VipInventory
        def eip = createEip {
            name = "eip"
            vipUuid = vip.uuid
            vmNicUuid = vmNic.uuid
        } as EipInventory

        VirtualRouterCommands.RemoveDhcpEntryCmd removeDhcpEntryCmd = null
        env.afterSimulator(VirtualRouterConstant.VR_REMOVE_DHCP_PATH) { VirtualRouterCommands.RemoveDhcpEntryRsp rsp, HttpEntity<String> e ->
                removeDhcpEntryCmd = json(e.body, VirtualRouterCommands.RemoveDhcpEntryCmd.class)
            return rsp
        }
        VirtualRouterCommands.AddDhcpEntryCmd addDhcpEntryCmd = null
        env.afterSimulator(VirtualRouterConstant.VR_ADD_DHCP_PATH) { VirtualRouterCommands.AddDhcpEntryRsp rsp, HttpEntity<String> e ->
                addDhcpEntryCmd = json(e.body, VirtualRouterCommands.AddDhcpEntryCmd.class)
            return rsp
        }
        VirtualRouterCommands.RemoveEipCmd removeEipCmd = null
        env.afterSimulator(VirtualRouterConstant.VR_REMOVE_EIP) { rsp, HttpEntity<String> entity ->
            removeEipCmd = json(entity.getBody(), VirtualRouterCommands.RemoveEipCmd)
            return rsp
        }
        VirtualRouterCommands.CreateEipCmd createEipCmd = null
        env.afterSimulator(VirtualRouterConstant.VR_CREATE_EIP) { rsp, HttpEntity<String> entity ->
            createEipCmd = json(entity.getBody(), VirtualRouterCommands.CreateEipCmd)
            return rsp
        }

        KVMAgentCommands.ApplySecurityGroupRuleCmd cmd = null
        env.afterSimulator(KVMSecurityGroupBackend.SECURITY_GROUP_APPLY_RULE_PATH) { rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.ApplySecurityGroupRuleCmd.class)
            return rsp
        }


        List<FreeIpInventory> freeIp4s = getFreeIp {
            l3NetworkUuid = vr_l3.uuid
            ipVersion = IPv6Constants.IPv4
        }
        String ip2 = freeIp4s.get(0).getIp()
        setVmStaticIp {
            vmInstanceUuid = vm.uuid
            l3NetworkUuid = vr_l3.uuid
            ip = ip2
        }

        assert ip1 != ip2

        retryInSecs {
            assert removeDhcpEntryCmd != null
            assert removeDhcpEntryCmd.dhcpEntries.get(0).ip == ip1
            assert addDhcpEntryCmd != null
            assert addDhcpEntryCmd.dhcpEntries.get(0).ip == ip2

            assert removeEipCmd != null
            assert removeEipCmd.eip.guestIp == ip1
            assert createEipCmd != null
            assert createEipCmd.eip.guestIp == ip2

            assert cmd != null
            assert cmd.ruleTOs.get(sg.uuid) != null
        }
    }

    void testBaseServiceWhenChangeL3() {
        L3NetworkInventory vr_l3 = env.inventoryByName("vrL3")
        L3NetworkInventory vr_l3_2 = env.inventoryByName("vrL3_2")
        L3NetworkInventory pub_l3 = env.inventoryByName("pubL3")
        VmInstanceInventory vm = createVmInstance {
            name = "vm-dhcp-change-ip"
            imageUuid = env.inventoryByName("image1").uuid
            instanceOfferingUuid = env.inventoryByName("instanceOffering").uuid
            l3NetworkUuids = [vr_l3.uuid]
        }
        VmNicInventory vmNic = vm.getVmNics().get(0)
        VmNicVO vmNicVO = dbFindByUuid(vmNic.uuid, VmNicVO.class)
        String ip1 = vmNicVO.getIp()

        def sg = createSecurityGroup {
            name = "sg-1"
            ipVersion = 4
        } as SecurityGroupInventory
        attachSecurityGroupToL3Network {
            securityGroupUuid = sg.uuid
            l3NetworkUuid = vr_l3.uuid
        }
        addVmNicToSecurityGroup {
            securityGroupUuid = sg.uuid
            vmNicUuids = [vmNic.uuid]
        }
        VirtualRouterCommands.RemoveDhcpEntryCmd removeDhcpEntryCmd = null
        env.afterSimulator(VirtualRouterConstant.VR_REMOVE_DHCP_PATH) { VirtualRouterCommands.RemoveDhcpEntryRsp rsp, HttpEntity<String> e ->
                removeDhcpEntryCmd = json(e.body, VirtualRouterCommands.RemoveDhcpEntryCmd.class)
            return rsp
        }
        VirtualRouterCommands.AddDhcpEntryCmd addDhcpEntryCmd = null
        env.afterSimulator(VirtualRouterConstant.VR_ADD_DHCP_PATH) { VirtualRouterCommands.AddDhcpEntryRsp rsp, HttpEntity<String> e ->
                addDhcpEntryCmd = json(e.body, VirtualRouterCommands.AddDhcpEntryCmd.class)
            return rsp
        }

        KVMAgentCommands.ApplySecurityGroupRuleCmd cmd = null
        env.afterSimulator(KVMSecurityGroupBackend.SECURITY_GROUP_APPLY_RULE_PATH) { rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.ApplySecurityGroupRuleCmd.class)
            return rsp
        }

        List<FreeIpInventory> freeIp4s = getFreeIp {
            l3NetworkUuid = vr_l3_2.uuid
            ipVersion = IPv6Constants.IPv4
        }
        String ip2 = freeIp4s.get(0).getIp()
        changeVmNicNetwork {
            vmNicUuid = vmNic.uuid
            destL3NetworkUuid = vr_l3_2.uuid
            systemTags = [String.format("staticIp::%s::%s", vr_l3_2.uuid, ip2)]
        }
        
        assert ip1 != ip2

        retryInSecs {
            assert removeDhcpEntryCmd != null
            assert removeDhcpEntryCmd.dhcpEntries.get(0).ip == ip1
            assert addDhcpEntryCmd != null
            assert addDhcpEntryCmd.dhcpEntries.get(0).ip == ip2

            assert cmd != null
            assert cmd.vmNicTOs.get(0).actionCode == VmNicSecurityTO.ACTION_CODE_DELETE_CHAIN
        }
    }
}