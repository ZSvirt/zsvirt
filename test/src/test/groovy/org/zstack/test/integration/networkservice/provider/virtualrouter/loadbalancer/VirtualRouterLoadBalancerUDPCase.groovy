package org.zstack.test.integration.networkservice.provider.virtualrouter.loadbalancer

import org.springframework.http.HttpEntity
import org.zstack.appliancevm.ApplianceVmVO
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.Q
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.lb.LoadBalancerConstants
import org.zstack.network.service.lb.LoadBalancerVO
import org.zstack.network.service.portforwarding.PortForwardingConstant
import org.zstack.network.service.vip.VipVO
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO
import org.zstack.network.service.virtualrouter.lb.VirtualRouterLoadBalancerBackend
import org.zstack.network.service.virtualrouter.lb.VirtualRouterLoadBalancerRefVO
import org.zstack.network.service.virtualrouter.vyos.VyosConstants
import org.zstack.sdk.*
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

import java.util.stream.Collectors
/**
 * Created by heathhose on 17-5-5.
 */
class VirtualRouterLoadBalancerUDPCase extends SubCase{
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

                    listener {
                        protocol = "udp"
                        loadBalancerPort = 8000
                        instancePort = 8000
                        useVmNic("vm", "l3")
                        useVmNic("vm2", "l3")
                        systemTags = ["healthCheckTarget::udp:8000"]
                    }
                    listener {
                        protocol = "tcp"
                        loadBalancerPort = 10000
                        instancePort = 10000
                        useVmNic("vm", "l3")
                        useVmNic("vm2", "l3")
                    }
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
        }
    }

    @Override
    void test() {
        dbf = bean(DatabaseFacade.class)
        env.create {
            TestCreateLoadBalancerListener()

            TestVirtualRouterDownReconnectVm()
            TestLoadBalancerListenerReservedPort()
            //TestUpdateLoadBalancerCase()
            //TestDeleteLoadBalancerListener()
        }
    }

    void TestCreateLoadBalancerListener() {
        L3NetworkInventory pubL3 = env.inventoryByName("pubL3")
        VmInstanceInventory vm = env.inventoryByName("vm")
        LoadBalancerInventory lb = env.inventoryByName("lb")

        LoadBalancerListenerInventory listener = createLoadBalancerListener {
            protocol = "udp"
            loadBalancerUuid = lb.uuid
            loadBalancerPort = 10000
            instancePort = 10000
            name = "test-listener"
            //systemTags = ["healthCheckTarget::udp:22"]
        }
        def count = 0
        VirtualRouterLoadBalancerBackend.RefreshLbCmd cmd = null
        env.afterSimulator(VirtualRouterLoadBalancerBackend.REFRESH_LB_PATH) { rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, VirtualRouterLoadBalancerBackend.RefreshLbCmd.class)
            count++
            return rsp
        }

        addVmNicToLoadBalancer {
            listenerUuid = listener.uuid
            vmNicUuids = [vm.getVmNics().get(0).getUuid()]
        }

        assert count == 1
        assert cmd.lbs.size() == 3
        count = 0
        for (VirtualRouterLoadBalancerBackend.LbTO lbtemp in cmd.lbs) {
            if (lbtemp.getParameters().contains("healthCheckTarget::udp:default")) {
                count++
            }
        }
        assert count == 1

        deleteLoadBalancerListener {
            uuid = listener.uuid
        }
    }

    void TestVirtualRouterDownReconnectVm(){
        // test a lb with multiple listeners, once the vr destroyed and recreated,
        // database has only one VirtualRouterLoadBalancerRefVO for the lb and vr
        assert dbf.count(VirtualRouterLoadBalancerRefVO.class) == 1
        ApplianceVmVO vr = dbf.listAll(ApplianceVmVO.class).get(0)
        VmInstanceInventory vm = env.inventoryByName("vm")
        LoadBalancerInventory load = env.inventoryByName("lb")
        destroyVmInstance {
            uuid = vr.uuid
        }

        assert dbf.count(VirtualRouterVmVO.class) == 0
        assert dbf.count(VirtualRouterLoadBalancerRefVO.class) == 0

        rebootVmInstance {
            uuid = vm.uuid
        }

        List<VirtualRouterLoadBalancerRefVO> list = dbf.listAll(VirtualRouterLoadBalancerRefVO.class)
        assert list.size() == 1
        assert list.get(0).getLoadBalancerUuid() == load.getUuid()
        LoadBalancerVO lb = dbFindByUuid(list.get(0).getLoadBalancerUuid(),LoadBalancerVO.class)
        assert !lb.getListeners().isEmpty()
        assert lb.getListeners().size() == 2
        if (8000 == lb.getListeners().toArray()[0].getInstancePort()) {
            assert LoadBalancerConstants.LB_PROTOCOL_UDP == lb.getListeners().toArray()[0].getProtocol()
            assert LoadBalancerConstants.LB_PROTOCOL_TCP == lb.getListeners().toArray()[1].getProtocol()
        } else {
            assert LoadBalancerConstants.LB_PROTOCOL_UDP == lb.getListeners().toArray()[1].getProtocol()
            assert LoadBalancerConstants.LB_PROTOCOL_TCP == lb.getListeners().toArray()[0].getProtocol()
        }
    }

    void TestLoadBalancerListenerReservedPort() {
        LoadBalancerInventory lb = env.inventoryByName("lb")

        /* normal vip can used all reserved port */
        createLoadBalancerListener {
            protocol = "udp"
            loadBalancerUuid = lb.uuid
            loadBalancerPort = 53
            instancePort = 10000
            name = "test-listener"
        }

        createLoadBalancerListener {
            protocol = "tcp"
            loadBalancerUuid = lb.uuid
            loadBalancerPort = 53
            instancePort = 10000
            name = "test-listener"
        }

        createLoadBalancerListener {
            protocol = "udp"
            loadBalancerUuid = lb.uuid
            loadBalancerPort = 68
            instancePort = 10000
            name = "test-listener"
        }

        createLoadBalancerListener {
            protocol = "udp"
            loadBalancerUuid = lb.uuid
            loadBalancerPort = 500
            instancePort = 10000
            name = "test-listener"
        }

        createLoadBalancerListener {
            protocol = "udp"
            loadBalancerUuid = lb.uuid
            loadBalancerPort = 4500
            instancePort = 10000
            name = "test-listener"
        }

        /* system vip can not use reserved port */
        VirtualRouterVmInventory vr = queryVirtualRouterVm {}[0]
        VmNicInventory pubNic = vr.getVmNics().stream().filter{nic -> nic.l3NetworkUuid == vr.getPublicNetworkUuid()}.collect(Collectors.toList()) [0]
        VipVO pubVip = Q.New(VipVO.class).list().stream().filter{ v -> v.ip == pubNic.ip}.collect(Collectors.toList()) [0]
        LoadBalancerInventory lb2 = createLoadBalancer {
            name = "test-lb-2"
            vipUuid = pubVip.uuid
        }

        expect (AssertionError.class) {
            createLoadBalancerListener {
                protocol = "udp"
                loadBalancerUuid = lb2.uuid
                loadBalancerPort = 53
                instancePort = 10000
                name = "test-listener"
            }
        }

        expect (AssertionError.class) {
            createLoadBalancerListener {
                protocol = "tcp"
                loadBalancerUuid = lb2.uuid
                loadBalancerPort = 53
                instancePort = 10000
                name = "test-listener"
            }
        }

        expect (AssertionError.class) {
            createLoadBalancerListener {
                protocol = "udp"
                loadBalancerUuid = lb2.uuid
                loadBalancerPort = 123
                instancePort = 10000
                name = "test-listener"
            }
        }
    }

    @Override
    void clean() {
        env.delete()
    }

}
