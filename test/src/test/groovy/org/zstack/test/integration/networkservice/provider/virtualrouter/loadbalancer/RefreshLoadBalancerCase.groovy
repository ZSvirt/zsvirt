package org.zstack.test.integration.networkservice.provider.virtualrouter.loadbalancer

import org.springframework.http.HttpEntity
import org.zstack.core.db.Q
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.lb.LoadBalancerConstants
import org.zstack.network.service.lb.LoadBalancerListenerVmNicRefVO
import org.zstack.network.service.lb.LoadBalancerListenerVmNicRefVO_
import org.zstack.network.service.lb.LoadBalancerVmNicStatus
import org.zstack.network.service.portforwarding.PortForwardingConstant
import org.zstack.network.service.virtualrouter.lb.VirtualRouterLoadBalancerBackend
import org.zstack.network.service.virtualrouter.vyos.VyosConstants
import org.zstack.sdk.*
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil
/**
 * Created by kayo on 2018/5/10.
 */
class RefreshLoadBalancerCase extends SubCase {
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
        env.create {
            testLoadBalanceRefresh()
        }
    }

    void testLoadBalanceRefresh() {
        L3NetworkInventory pubL3 = env.inventoryByName("pubL3")
        VmInstanceInventory vm = env.inventoryByName("vm")

        VipInventory vip = createVip {
            name = "test-vip"
            l3NetworkUuid = pubL3.uuid
        }
        LoadBalancerInventory lb = createLoadBalancer {
            name = "test-lb"
            vipUuid = vip.uuid
        }

        def listener1 = createLoadBalancerListener {
            loadBalancerUuid = lb.uuid
            loadBalancerPort = 44
            instancePort = 22
            name = "test-listener"
        } as LoadBalancerListenerInventory

        addVmNicToLoadBalancer {
            vmNicUuids = [vm.getVmNics().get(0).uuid]
            listenerUuid = listener1.uuid
        }

        def listener = createLoadBalancerListener {
            loadBalancerUuid = lb.uuid
            loadBalancerPort = 33
            instancePort = 22
            name = "test-listener"
        } as LoadBalancerListenerInventory

        addVmNicToLoadBalancer {
            vmNicUuids = [vm.getVmNics().get(0).uuid]
            listenerUuid = listener.uuid
        }

        listener = createLoadBalancerListener {
            loadBalancerUuid = lb.uuid
            loadBalancerPort = 55
            instancePort = 44
            name = "test-listener"
        } as LoadBalancerListenerInventory

        addVmNicToLoadBalancer {
            vmNicUuids = [vm.getVmNics().get(0).uuid]
            listenerUuid = listener.uuid
        }

        VirtualRouterLoadBalancerBackend.RefreshLbCmd cmd = null
        def count = 0
        env.afterSimulator(VirtualRouterLoadBalancerBackend.REFRESH_LB_PATH) { rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, VirtualRouterLoadBalancerBackend.RefreshLbCmd.class)
            count++
            return rsp
        }

        stopVmInstance {
            uuid = vm.uuid
        }

        // listeners with same nic on same load balance only refresh rules one time
        assert count == 1
        assert cmd.lbs.size() == 3

        def vm2 = env.inventoryByName("vm2") as VmInstanceInventory

        addVmNicToLoadBalancer {
            vmNicUuids = [vm2.getVmNics().get(0).uuid]
            listenerUuid = listener.uuid
        }

        cmd = null
        count = 0

        stopVmInstance {
            uuid = vm2.uuid
        }

        assert count == 1
        assert cmd.lbs.size() == 3

        // one lb doesn't have nic
        removeVmNicFromLoadBalancer {
            vmNicUuids = [vm.getVmNics().get(0).uuid]
            listenerUuid = listener.uuid
        }

        removeVmNicFromLoadBalancer {
            vmNicUuids = [vm.getVmNics().get(0).uuid]
            listenerUuid = listener1.uuid
        }

        cmd = null
        count = 0

        startVmInstance {
            uuid = vm.uuid
        }

        assert count == 1
        assert cmd.lbs.size() == 3

        // no load balancer rules won't refresh rules
        removeVmNicFromLoadBalancer {
            vmNicUuids = [vm2.getVmNics().get(0).uuid]
            listenerUuid = listener.uuid
        }

        cmd = null
        count = 0

        stopVmInstance {
            uuid = vm.uuid
        }

        assert count == 1
        assert cmd.lbs.size() == 3


        VipInventory vip2 = createVip {
            name = "test-vip2"
            l3NetworkUuid = pubL3.uuid
        }

        LoadBalancerInventory lb2 = createLoadBalancer {
            name = "test-lb"
            vipUuid = vip.uuid
        }

        def listener2 = createLoadBalancerListener {
            loadBalancerUuid = lb2.uuid
            loadBalancerPort = 77
            instancePort = 88
            name = "test-listener"
        } as LoadBalancerListenerInventory

        addVmNicToLoadBalancer {
            vmNicUuids = [vm.getVmNics().get(0).uuid]
            listenerUuid = listener2.uuid
        }

        env.cleanAfterSimulatorHandlers()

        def countInvoked = 0
        def rulesSize = 0
        env.afterSimulator(VirtualRouterLoadBalancerBackend.REFRESH_LB_PATH) { rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, VirtualRouterLoadBalancerBackend.RefreshLbCmd.class)

            countInvoked++
            rulesSize += cmd.lbs.size()

            return rsp
        }

        startVmInstance {
            uuid = vm.uuid
        }

        assert countInvoked == 2
        assert rulesSize == 4
    }
}
