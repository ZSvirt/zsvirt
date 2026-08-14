package org.zstack.test.integration.networkservice.provider.virtualrouter.loadbalancer

import org.springframework.http.HttpEntity
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.lb.LoadBalancerConstants
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
 * Created by shixin.ruan on 2018/08/14.
 */
class LoadBalancerDeleteVmCase extends SubCase {
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
                name = "vm-1"
                useImage("image")
                useL3Networks("l3")
                useInstanceOffering("instanceOffering")
            }

            vm {
                name = "vm-2"
                useImage("image")
                useL3Networks("l3")
                useInstanceOffering("instanceOffering")
            }
        }
    }

    @Override
    void test() {
        env.create {
            testDeleteVm()
        }
    }

    void testDeleteVm() {
        L3NetworkInventory pubL3 = env.inventoryByName("pubL3")
        VmInstanceInventory vm1 = env.inventoryByName("vm-1")
        VmInstanceInventory vm2 = env.inventoryByName("vm-2")

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
            loadBalancerPort = 100
            instancePort = 100
            name = "test-listener-1"
        } as LoadBalancerListenerInventory

        def listener2 = createLoadBalancerListener {
            loadBalancerUuid = lb.uuid
            loadBalancerPort = 200
            instancePort = 200
            name = "test-listener-2"
        } as LoadBalancerListenerInventory

        def listener3 = createLoadBalancerListener {
            loadBalancerUuid = lb.uuid
            loadBalancerPort = 300
            instancePort = 300
            name = "test-listener-3"
        } as LoadBalancerListenerInventory

        addVmNicToLoadBalancer {
            vmNicUuids = [vm1.getVmNics().get(0).uuid]
            listenerUuid = listener1.uuid
        }
        addVmNicToLoadBalancer {
            vmNicUuids = [vm1.getVmNics().get(0).uuid]
            listenerUuid = listener2.uuid
        }
        addVmNicToLoadBalancer {
            vmNicUuids = [vm1.getVmNics().get(0).uuid]
            listenerUuid = listener3.uuid
        }

        addVmNicToLoadBalancer {
            vmNicUuids = [vm2.getVmNics().get(0).uuid]
            listenerUuid = listener1.uuid
        }
        addVmNicToLoadBalancer {
            vmNicUuids = [vm2.getVmNics().get(0).uuid]
            listenerUuid = listener2.uuid
        }
        addVmNicToLoadBalancer {
            vmNicUuids = [vm2.getVmNics().get(0).uuid]
            listenerUuid = listener3.uuid
        }

        List<VirtualRouterLoadBalancerBackend.RefreshLbCmd> cmds = new ArrayList<>()
        env.afterSimulator(VirtualRouterLoadBalancerBackend.REFRESH_LB_PATH) { rsp, HttpEntity<String> e ->
            VirtualRouterLoadBalancerBackend.RefreshLbCmd cmd =
                    JSONObjectUtil.toObject(e.body, VirtualRouterLoadBalancerBackend.RefreshLbCmd.class)
            cmds.add(cmd)
            return rsp
        }

        def thread1 = Thread.start {
            destroyVmInstance {
                uuid = vm1.uuid
            }
        }
        def thread2 = Thread.start {
            destroyVmInstance {
                uuid = vm2.uuid
            }
        }
        [thread1, thread2].each {it.join()}

        assert cmds.size() == 2
        VirtualRouterLoadBalancerBackend.RefreshLbCmd cmd1 = cmds.get(0)
        VirtualRouterLoadBalancerBackend.RefreshLbCmd cmd2 = cmds.get(1)
        assert cmd1.lbs.size() == 3
        assert cmd2.lbs.size() == 3
        VirtualRouterLoadBalancerBackend.LbTO lbTo1 = cmd1.lbs.get(0)
        VirtualRouterLoadBalancerBackend.LbTO lbTo2 = cmd1.lbs.get(1)
        VirtualRouterLoadBalancerBackend.LbTO lbTo3 = cmd1.lbs.get(2)
        assert lbTo1.nicIps.size() == lbTo2.nicIps.size()
        assert lbTo1.nicIps.size() == lbTo3.nicIps.size()

        VirtualRouterLoadBalancerBackend.LbTO lbTo4 = cmd2.lbs.get(0)
        VirtualRouterLoadBalancerBackend.LbTO lbTo5 = cmd2.lbs.get(1)
        VirtualRouterLoadBalancerBackend.LbTO lbTo6 = cmd2.lbs.get(2)
        assert lbTo4.nicIps.size() == lbTo5.nicIps.size()
        assert lbTo4.nicIps.size() == lbTo6.nicIps.size()

        assert lbTo1.nicIps.size() == 1 || lbTo1.nicIps.size() == 0
        if (lbTo1.nicIps.size() == 1) {
            assert lbTo4.nicIps.size() == 0
        } else {
            assert lbTo4.nicIps.size() == 1
        }

        LoadBalancerInventory lb1 = queryLoadBalancer {conditions=["uuid=${lb.uuid}".toString()]} [0]
        assert lb1.listeners.size() == 3
        LoadBalancerListenerInventory lbl1 = lb1.listeners.get(0)
        LoadBalancerListenerInventory lbl2 = lb1.listeners.get(1)
        LoadBalancerListenerInventory lbl3 = lb1.listeners.get(2)
        assert lbl1.vmNicRefs.size() == 0
        assert lbl2.vmNicRefs.size() == 0
        assert lbl3.vmNicRefs.size() == 0
   }
}
