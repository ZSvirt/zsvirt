package org.zstack.test.integration.networkservice.provider.virtualrouter.loadbalancer

import org.springframework.http.HttpEntity
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.Q
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.lb.LoadBalancerConstants
import org.zstack.network.service.lb.LoadBalancerListenerACLRefVO
import org.zstack.network.service.portforwarding.PortForwardingConstant
import org.zstack.network.service.virtualrouter.lb.VirtualRouterLoadBalancerBackend
import org.zstack.network.service.virtualrouter.vyos.VyosConstants
import org.zstack.sdk.*
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

import java.util.stream.Collectors

class LoadBalancerRedirectRuleCase extends SubCase {
    DatabaseFacade dbf
    EnvSpec env

    @Override
    void setup() {
        useSpring(NetworkServiceProviderTest.springSpec)
    }

    @Override
    void environment() {
        env = env {

            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(8)
                cpu = 4
            }

            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                username = "root"
                password = "password"
                hostname = "localhost"

                image {
                    name = "image"
                    url = "http://zstack.org/download/test.qcow2"
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
                        managementIp = "localhost"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("local")
                    attachL2Network("l2")
                }

                localPrimaryStorage {
                    name = "local"
                    url = "/local_ps"
                }

                l2NoVlanNetwork {
                    name = "l2"
                    physicalInterface = "eth0"

                    l3Network {
                        name = "l3"

                        service {
                            provider = VyosConstants.VYOS_ROUTER_PROVIDER_TYPE
                            types = [NetworkServiceType.DHCP.toString(),
                                     NetworkServiceType.DNS.toString(),
                                     NetworkServiceType.SNAT.toString(),
                                     PortForwardingConstant.PORTFORWARDING_NETWORK_SERVICE_TYPE,
                                     LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE_STRING,
                                     EipConstant.EIP_NETWORK_SERVICE_TYPE]
                        }

                        ip {
                            startIp = "192.168.100.10"
                            endIp = "192.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.100.1"
                        }
                    }

                    l3Network {
                        name = "pubL3"

                        ip {
                            startIp = "11.168.100.10"
                            endIp = "11.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "11.168.100.1"
                        }
                    }
                }

                attachBackupStorage("sftp")

                virtualRouterOffering {
                    name = "vro"
                    memory = SizeUnit.MEGABYTE.toByte(512)
                    cpu = 2
                    useManagementL3Network("pubL3")
                    usePublicL3Network("pubL3")
                    useImage("vr")
                }

                lb {
                    name = "lb"
                    useVip("pubL3")
                }
            }

            vm {
                name = "vm"
                useImage("image")
                useL3Networks("l3")
                useInstanceOffering("instanceOffering")
            }

            vm {
                name = "vm2"
                useImage("image")
                useL3Networks("l3")
                useInstanceOffering("instanceOffering")
            }

            vm {
                name = "vm3"
                useImage("image")
                useL3Networks("l3")
                useInstanceOffering("instanceOffering")
            }
        }
    }

    @Override
    void clean() {
        def load = env.inventoryByName("lb") as LoadBalancerInventory
        deleteLoadBalancer {
            uuid = load.uuid
        }
        assert Q.New(LoadBalancerListenerACLRefVO.class).count() == 0
        env.delete()
    }

    @Override
    void test() {
        dbf = bean(DatabaseFacade.class)
        env.create {
            testRulePort()
            testOnlyUrlOrDomain()
        }
    }


    void testRulePort(){
        def load = env.inventoryByName("lb") as LoadBalancerInventory
        def vm = env.inventoryByName("vm") as VmInstanceInventory
        def vm2 = env.inventoryByName("vm2") as VmInstanceInventory
        def vm3 = env.inventoryByName("vm3") as VmInstanceInventory
        def l3 = env.inventoryByName("l3") as L3NetworkInventory

        VirtualRouterLoadBalancerBackend.RefreshLbCmd cmd = null
        env.afterSimulator(VirtualRouterLoadBalancerBackend.REFRESH_LB_PATH) { rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, VirtualRouterLoadBalancerBackend.RefreshLbCmd.class)
            return rsp
        }

        LoadBalancerListenerInventory lbl = createLoadBalancerListener {
            protocol = LoadBalancerConstants.LB_PROTOCOL_HTTP
            loadBalancerUuid = load.uuid
            loadBalancerPort = 80
            instancePort = 100
            name = "test-listener"
        }

        LoadBalancerServerGroupInventory sg1 = createLoadBalancerServerGroup{
            loadBalancerUuid =  load.uuid
            name = "sg1"
        }

        addServerGroupToLoadBalancerListener {
            listenerUuid = lbl.uuid
            serverGroupUuid = sg1.uuid
        }


        AccessControlListInventory acl1 = createAccessControlList {
            name = "acl1"
        }

        AccessControlListEntryInventory redirectRule1 = addAccessControlListRedirectRule {
            name = "redirect rule"
            domain = "zstack.io"
            url = "/test"
            aclUuid = acl1.uuid
        }

        addAccessControlListToLoadBalancer {
            aclUuids = [acl1.uuid]
            aclType = "redirect"
            listenerUuid = lbl.uuid
            serverGroupUuids = [sg1.uuid]
        }

        VmNicInventory nic1 = vm.vmNics.get(0)

        addBackendServerToServerGroup {
            vmNics = [['uuid':nic1.uuid,'weight':'10']]
            serverGroupUuid = sg1.uuid
        }

        def lbTO = cmd.lbs.stream().filter{lb -> lb.listenerUuid.equals(lbl.uuid)}.collect(Collectors.toList())[0] as VirtualRouterLoadBalancerBackend.LbTO
        def redirectRules= lbTO.redirectRules.stream().filter{ it -> it.getServerGroupUuid().equals(sg1.uuid)}.collect(Collectors.toList())
        assert !redirectRules[0].redirectRule.contains(":80")
        assert redirectRules[1].redirectRule.contains(":80")

        deleteAccessControlList {
            uuid = acl1.uuid
        }

        deleteLoadBalancerListener {
            uuid = lbl.uuid
        }
    }

    void testOnlyUrlOrDomain(){
        def load = env.inventoryByName("lb") as LoadBalancerInventory
        def vm = env.inventoryByName("vm") as VmInstanceInventory
        def l3 = env.inventoryByName("l3") as L3NetworkInventory

        VirtualRouterLoadBalancerBackend.RefreshLbCmd cmd = null
        env.afterSimulator(VirtualRouterLoadBalancerBackend.REFRESH_LB_PATH) { rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, VirtualRouterLoadBalancerBackend.RefreshLbCmd.class)
            return rsp
        }

        LoadBalancerListenerInventory lbl = createLoadBalancerListener {
            protocol = LoadBalancerConstants.LB_PROTOCOL_HTTP
            loadBalancerUuid = load.uuid
            loadBalancerPort = 8093
            instancePort = 80
            name = "test-listener"
        }

        LoadBalancerServerGroupInventory sg1 = createLoadBalancerServerGroup{
            loadBalancerUuid =  load.uuid
            name = "sg1"
        }

        addServerGroupToLoadBalancerListener {
            listenerUuid = lbl.uuid
            serverGroupUuid = sg1.uuid
        }


        AccessControlListInventory acl1 = createAccessControlList {
            name = "acl1"
        }

        AccessControlListEntryInventory redirectRule1 = addAccessControlListRedirectRule {
            name = "redirect rule"
            domain = ""
            url = "/test"
            aclUuid = acl1.uuid
        }

        addAccessControlListToLoadBalancer {
            aclUuids = [acl1.uuid]
            aclType = "redirect"
            listenerUuid = lbl.uuid
            serverGroupUuids = [sg1.uuid]
        }

        VmNicInventory nic1 = vm.vmNics.get(0)

        addBackendServerToServerGroup {
            vmNics = [['uuid':nic1.uuid,'weight':'10']]
            serverGroupUuid = sg1.uuid
        }

        def lbTO = cmd.lbs.stream().filter{lb -> lb.listenerUuid.equals(lbl.uuid)}.collect(Collectors.toList())[0] as VirtualRouterLoadBalancerBackend.LbTO
        def redirectRules= lbTO.redirectRules.stream().filter{ it -> it.getServerGroupUuid().equals(sg1.uuid)}.collect(Collectors.toList())
        assert redirectRules.size == 1
        assert redirectRules[0].redirectRule.contains("path")
        assert !redirectRules[0].redirectRule.contains(":8093")

        deleteAccessControlList {
            uuid = acl1.uuid
        }


        AccessControlListInventory acl2 = createAccessControlList {
            name = "acl2"
        }

        AccessControlListEntryInventory redirectRule2 = addAccessControlListRedirectRule {
            name = "redirect rule"
            domain = "zstack.io"
            url = ""
            aclUuid = acl2.uuid
        }

        addAccessControlListToLoadBalancer {
            aclUuids = [acl2.uuid]
            aclType = "redirect"
            listenerUuid = lbl.uuid
            serverGroupUuids = [sg1.uuid]
        }

        def lbTO2 = cmd.lbs.stream().filter{lb -> lb.listenerUuid.equals(lbl.uuid)}.collect(Collectors.toList())[0] as VirtualRouterLoadBalancerBackend.LbTO
        redirectRules = lbTO2.redirectRules.stream().filter{ it -> it.getServerGroupUuid().equals(sg1.uuid)}.collect(Collectors.toList())
        assert redirectRules.size == 1
        assert redirectRules[0].redirectRule.contains("hdr")
        assert !redirectRules[0].redirectRule.contains(":8093")

        deleteAccessControlList {
            uuid = acl2.uuid
        }


        deleteLoadBalancerListener {
            uuid = lbl.uuid
        }
    }

}

