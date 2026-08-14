package org.zstack.test.integration.networkservice.provider.virtualrouter.loadbalancer

import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.vm.VmInstanceVO_
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.lb.LoadBalancerListenerVO
import org.zstack.network.service.lb.LoadBalancerListenerVO_
import org.zstack.network.service.lb.LoadBalancerConstants
import org.zstack.network.service.lb.LoadBalancerVO
import org.zstack.network.service.lb.LoadBalancerVO_
import org.zstack.network.service.portforwarding.PortForwardingConstant
import org.zstack.network.service.vip.VipNetworkServicesRefVO
import org.zstack.network.service.vip.VipNetworkServicesRefVO_
import org.zstack.network.service.virtualrouter.vyos.VyosConstants
import org.zstack.sdk.*
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

import static java.util.Arrays.asList

/**
 * Created by camile on 2017/5/19.
 */
class UpdateLoadBalancerListenerCase extends SubCase {
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
                        totalCpu = 8
                        totalMem = SizeUnit.GIGABYTE.toByte(12)
                    }

                    attachPrimaryStorage("local")
                    attachL2Network("PRIVATE-L2")
                    attachL2Network("PUBLIC-MANAGEMENT-L2")
                }
                localPrimaryStorage {
                    name = "local"
                    url = "/local_ps"
                }
                l2NoVlanNetwork {
                    name = "PUBLIC-MANAGEMENT-L2"
                    physicalInterface = "eth0"
                    l3Network {
                        name = "PUBLIC-MANAGEMENT-L3"
                        ip {
                            startIp = "172.20.57.160"
                            endIp = "172.20.57.200"
                            gateway = "172.20.0.1"
                            netmask = "255.255.0.0"
                        }
                    }
                }

                l2VlanNetwork {
                    name = "PRIVATE-L2"
                    physicalInterface = "eth1"
                    vlan = 100
                    l3Network {
                        name = "PRIVATE-L3"
                        ip {
                            startIp = "10.10.2.100"
                            endIp = "10.20.2.200"
                            gateway = "10.10.2.1"
                            netmask = "255.0.0.0"
                        }
                        service {
                            provider = VyosConstants.VYOS_ROUTER_PROVIDER_TYPE
                            types = [NetworkServiceType.DHCP.toString(),
                                     NetworkServiceType.DNS.toString(),
                                     NetworkServiceType.SNAT.toString(),
                                     PortForwardingConstant.PORTFORWARDING_NETWORK_SERVICE_TYPE,
                                     LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE_STRING,
                                     EipConstant.EIP_NETWORK_SERVICE_TYPE]
                        }
                    }
                }
                attachBackupStorage("sftp")

                virtualRouterOffering {
                    name = "vro"
                    memory = SizeUnit.MEGABYTE.toByte(512)
                    cpu = 2
                    useManagementL3Network("PUBLIC-MANAGEMENT-L3")
                    usePublicL3Network("PUBLIC-MANAGEMENT-L3")
                    useImage("vr")
                    isDefault = true
                }
            }
            vm {
                name = "vm1"
                useImage("image")
                useL3Networks("PRIVATE-L3")
                useInstanceOffering("instanceOffering")
            }
            vm {
                name = "vm2"
                useImage("image")
                useL3Networks("PRIVATE-L3")
                useInstanceOffering("instanceOffering")
            }
        }

    }

    @Override
    void test() {
        env.create {
            testUpdateLoadBalancerListener()
            testUpdateLBListenerwithSamePort()
            testDeleteLBListener()
            cleanEnvironment()
        }
    }

    void testUpdateLoadBalancerListener() {
        L3NetworkInventory publicL3 = env.inventoryByName("PUBLIC-MANAGEMENT-L3") as L3NetworkInventory
        VmInstanceInventory vm1 = env.inventoryByName("vm1") as VmInstanceInventory
        VmInstanceInventory vm2 = env.inventoryByName("vm2") as VmInstanceInventory
        VmNicInventory vm1Nic1 = vm1.vmNics.get(0)
        VmNicInventory vm2Nic1 = vm2.vmNics.get(0)

        CreateVipAction createVipAction = new CreateVipAction()
        createVipAction.name = "vip"
        createVipAction.l3NetworkUuid = publicL3.uuid
        createVipAction.sessionId = adminSession()
        CreateVipAction.Result res = createVipAction.call()
        assert res.error == null
        VipInventory vipInventory = res.value.inventory

        CreateEipAction createEipAction = new CreateEipAction()
        createEipAction.name = "vm1_eip"
        createEipAction.vipUuid = vipInventory.uuid
        createEipAction.vmNicUuid = vm1Nic1.uuid
        createEipAction.sessionId = adminSession()
        CreateEipAction.Result eipRes = createEipAction.call()
        assert eipRes.error == null
        EipInventory eipInventory = eipRes.value.inventory

        String vr1Uuid = Q.New(VmInstanceVO.class).select(VmInstanceVO_.uuid)
                .eq(VmInstanceVO_.type, "ApplianceVm")
                .findValue()
        assert vr1Uuid != null

        CreateVipAction createVipAction2 = new CreateVipAction()
        createVipAction2.name = "vip"
        createVipAction2.l3NetworkUuid = publicL3.uuid
        createVipAction2.sessionId = adminSession()
        CreateVipAction.Result creVipres = createVipAction2.call()
        assert creVipres.error == null
        VipInventory vipInventory2 = creVipres.value.inventory

        CreateLoadBalancerAction createLoadBalancerAction = new CreateLoadBalancerAction()
        createLoadBalancerAction.name = "carl"
        createLoadBalancerAction.vipUuid = vipInventory2.uuid
        createLoadBalancerAction.systemTags = asList("separateVirtualRouterVm")
        createLoadBalancerAction.sessionId = adminSession()
        CreateLoadBalancerAction.Result lbRes = createLoadBalancerAction.call()
        assert lbRes.error == null
        LoadBalancerInventory loadBalancerInventory = lbRes.value.inventory

        String desc = "desc"
        CreateLoadBalancerListenerAction createLoadBalancerListenerAction = new CreateLoadBalancerListenerAction()
        createLoadBalancerListenerAction.loadBalancerUuid = loadBalancerInventory.uuid
        createLoadBalancerListenerAction.loadBalancerPort = 22
        createLoadBalancerListenerAction.instancePort = 22
        createLoadBalancerListenerAction.name = "ssh"
        createLoadBalancerListenerAction.protocol = "tcp"
        createLoadBalancerListenerAction.sessionId = adminSession()
        createLoadBalancerListenerAction.description = desc
        CreateLoadBalancerListenerAction.Result lblRes = createLoadBalancerListenerAction.call()
        assert lblRes.error == null
        LoadBalancerListenerInventory loadBalancerListenerInventory = lblRes.value.inventory
        assert desc == loadBalancerListenerInventory.description

        UpdateLoadBalancerListenerAction action = new UpdateLoadBalancerListenerAction()
        action.uuid = loadBalancerListenerInventory.uuid
        action.name = "test2"
        action.description = "desc info2"
        action.sessionId = adminSession()
        UpdateLoadBalancerListenerAction.Result ubllRes = action.call()
        LoadBalancerListenerVO lblVo =
                Q.New(LoadBalancerListenerVO.class).eq(LoadBalancerListenerVO_.uuid, loadBalancerListenerInventory.uuid).find()
        assert ubllRes.error == null
        assert ubllRes.value.inventory.uuid == lblVo.uuid
        assert ubllRes.value.inventory.name == lblVo.name
        assert ubllRes.value.inventory.description == lblVo.description

        deleteVip {
            uuid = vipInventory.getUuid()
        }

        deleteVip {
            uuid = vipInventory2.getUuid()
        }

        deleteEip{
            uuid = eipInventory.getUuid()
        }

        deleteLoadBalancer {
            uuid = loadBalancerInventory.getUuid()
        }
    }

    void testUpdateLBListenerwithSamePort() {
        L3NetworkInventory publicL3 = env.inventoryByName("PUBLIC-MANAGEMENT-L3") as L3NetworkInventory

        CreateVipAction createVipAction1 = new CreateVipAction()
        createVipAction1.name = "vip-1"
        createVipAction1.l3NetworkUuid = publicL3.uuid
        createVipAction1.sessionId = adminSession()
        CreateVipAction.Result creVipres1 = createVipAction1.call()
        assert creVipres1.error == null
        VipInventory vipInventory1 = creVipres1.value.inventory

        CreateLoadBalancerAction createLoadBalancerAction1 = new CreateLoadBalancerAction()
        createLoadBalancerAction1.name = "lb-1"
        createLoadBalancerAction1.vipUuid = vipInventory1.uuid
        createLoadBalancerAction1.systemTags = asList("separateVirtualRouterVm")
        createLoadBalancerAction1.sessionId = adminSession()
        CreateLoadBalancerAction.Result lbRes1 = createLoadBalancerAction1.call()
        assert lbRes1.error == null
        LoadBalancerInventory loadBalancerInventory1 = lbRes1.value.inventory

        CreateLoadBalancerListenerAction createLoadBalancerListenerAction1 = new CreateLoadBalancerListenerAction()
        createLoadBalancerListenerAction1.loadBalancerUuid = loadBalancerInventory1.uuid
        createLoadBalancerListenerAction1.loadBalancerPort = 22
        createLoadBalancerListenerAction1.instancePort = 22
        createLoadBalancerListenerAction1.name = "ssh"
        createLoadBalancerListenerAction1.protocol = "tcp"
        createLoadBalancerListenerAction1.sessionId = adminSession()
        CreateLoadBalancerListenerAction.Result lblRes1 = createLoadBalancerListenerAction1.call()
        assert lblRes1.error == null
        LoadBalancerListenerInventory loadBalancerListenerInventory1 = lblRes1.value.inventory

        /* create another loadbalance listenter in lb-1 and use same instance Port, it will succeed
         */
        createLoadBalancerListenerAction1.instancePort = 22
        createLoadBalancerListenerAction1.loadBalancerPort = 222
        CreateLoadBalancerListenerAction.Result res = createLoadBalancerListenerAction1.call()
        assert res.error == null

        
        /*create lb-2 with same vip as lb-1, then attach loadBalancerPort woth same port number 22
         * it return an exception, then attach instancePort withe same port number, worked */
        CreateLoadBalancerAction createLoadBalancerAction2 = new CreateLoadBalancerAction()
        createLoadBalancerAction2.name = "lb-2"
        createLoadBalancerAction2.vipUuid = vipInventory1.uuid
        createLoadBalancerAction2.systemTags = asList("separateVirtualRouterVm")
        createLoadBalancerAction2.sessionId = adminSession()
        CreateLoadBalancerAction.Result lbRes2 = createLoadBalancerAction2.call()
        assert lbRes2.error == null
        LoadBalancerInventory loadBalancerInventory2 = lbRes2.value.inventory

        CreateLoadBalancerListenerAction createLoadBalancerListenerAction2 = new CreateLoadBalancerListenerAction()
        createLoadBalancerListenerAction2.loadBalancerUuid = loadBalancerInventory2.uuid
        createLoadBalancerListenerAction2.loadBalancerPort = 22
        createLoadBalancerListenerAction2.instancePort = 28
        createLoadBalancerListenerAction2.name = "ssh"
        createLoadBalancerListenerAction2.protocol = "tcp"
        createLoadBalancerListenerAction2.sessionId = adminSession()
        CreateLoadBalancerListenerAction.Result lblRes2 = createLoadBalancerListenerAction2.call()
        assert lblRes2.error != null
        // can't checkout concrete content of error message
        // assert lblRes2.error.details.indexOf("loadBalancerPort") > -1

        createLoadBalancerListenerAction2.loadBalancerPort = 23
        createLoadBalancerListenerAction2.instancePort = 22
        CreateLoadBalancerListenerAction.Result lblRes21 = createLoadBalancerListenerAction2.call()
        assert lblRes21.error == null


        /* delete lb-1, then create again, it will success */
        deleteLoadBalancer {
            uuid = loadBalancerInventory1.uuid
        }
        createLoadBalancerListenerAction2 = new CreateLoadBalancerListenerAction()
        createLoadBalancerListenerAction2.loadBalancerUuid = loadBalancerInventory2.uuid
        createLoadBalancerListenerAction2.loadBalancerPort = 22
        createLoadBalancerListenerAction2.instancePort = 22
        createLoadBalancerListenerAction2.name = "ssh"
        createLoadBalancerListenerAction2.protocol = "tcp"
        createLoadBalancerListenerAction2.sessionId = adminSession()
        lblRes2 = createLoadBalancerListenerAction2.call()
        assert lblRes2.error == null

        /* create lb-3 with different vip, then attach loadBalancerPort woth same port number 22
          * it will return success */
        CreateVipAction createVipAction3 = new CreateVipAction()
        createVipAction3.name = "vip-3"
        createVipAction3.l3NetworkUuid = publicL3.uuid
        createVipAction3.sessionId = adminSession()
        CreateVipAction.Result creVipres3 = createVipAction3.call()
        assert creVipres3.error == null
        VipInventory vipInventory3 = creVipres3.value.inventory
        
        CreateLoadBalancerAction createLoadBalancerAction3 = new CreateLoadBalancerAction()
        createLoadBalancerAction3.name = "lb-3"
        createLoadBalancerAction3.vipUuid = vipInventory3.uuid
        createLoadBalancerAction3.systemTags = asList("separateVirtualRouterVm")
        createLoadBalancerAction3.sessionId = adminSession()
        CreateLoadBalancerAction.Result lbRes3 = createLoadBalancerAction3.call()
        assert lbRes3.error == null
        LoadBalancerInventory loadBalancerInventory3 = lbRes3.value.inventory

        CreateLoadBalancerListenerAction createLoadBalancerListenerAction3 = new CreateLoadBalancerListenerAction()
        createLoadBalancerListenerAction3.loadBalancerUuid = loadBalancerInventory3.uuid
        createLoadBalancerListenerAction3.loadBalancerPort = 22
        createLoadBalancerListenerAction3.instancePort = 22
        createLoadBalancerListenerAction3.name = "ssh"
        createLoadBalancerListenerAction3.protocol = "tcp"
        createLoadBalancerListenerAction3.sessionId = adminSession()
        CreateLoadBalancerListenerAction.Result lblRes3 = createLoadBalancerListenerAction3.call()
        assert lblRes3.error == null
        LoadBalancerListenerInventory lblInv3 = lblRes3.value.inventory



        /* create loadbalancer with different port but same vip */
        CreateLoadBalancerListenerAction createLoadBalancerListenerAction4 = new CreateLoadBalancerListenerAction()
        createLoadBalancerListenerAction4.loadBalancerUuid = loadBalancerInventory3.uuid
        createLoadBalancerListenerAction4.loadBalancerPort = 23
        createLoadBalancerListenerAction4.instancePort = 23
        createLoadBalancerListenerAction4.name = "ssh"
        createLoadBalancerListenerAction4.protocol = "tcp"
        createLoadBalancerListenerAction4.sessionId = adminSession()
        CreateLoadBalancerListenerAction.Result lblRes4 = createLoadBalancerListenerAction4.call()
        assert lblRes4.error == null
    }

    void testDeleteLBListener() {
        L3NetworkInventory publicL3 = env.inventoryByName("PUBLIC-MANAGEMENT-L3") as L3NetworkInventory
        VmInstanceInventory vm1 = env.inventoryByName("vm1") as VmInstanceInventory
        VmInstanceInventory vm2 = env.inventoryByName("vm2") as VmInstanceInventory
        VmNicInventory vm1Nic1 = vm1.vmNics.get(0)
        VmNicInventory vm2Nic1 = vm2.vmNics.get(0)

        CreateVipAction createVipAction1 = new CreateVipAction()
        createVipAction1.name = "vip-1"
        createVipAction1.l3NetworkUuid = publicL3.uuid
        createVipAction1.sessionId = adminSession()
        CreateVipAction.Result creVipres1 = createVipAction1.call()
        assert creVipres1.error == null
        VipInventory vipInventory1 = creVipres1.value.inventory

        CreateLoadBalancerAction createLoadBalancerAction1 = new CreateLoadBalancerAction()
        createLoadBalancerAction1.name = "lb-1"
        createLoadBalancerAction1.vipUuid = vipInventory1.uuid
        createLoadBalancerAction1.sessionId = adminSession()
        CreateLoadBalancerAction.Result lbRes1 = createLoadBalancerAction1.call()
        assert lbRes1.error == null
        LoadBalancerInventory loadBalancerInventory1 = lbRes1.value.inventory

        CreateLoadBalancerListenerAction createLoadBalancerListenerAction1 = new CreateLoadBalancerListenerAction()
        createLoadBalancerListenerAction1.loadBalancerUuid = loadBalancerInventory1.uuid
        createLoadBalancerListenerAction1.loadBalancerPort = 22
        createLoadBalancerListenerAction1.instancePort = 22
        createLoadBalancerListenerAction1.name = "ssh"
        createLoadBalancerListenerAction1.protocol = "tcp"
        createLoadBalancerListenerAction1.sessionId = adminSession()
        CreateLoadBalancerListenerAction.Result lblRes1 = createLoadBalancerListenerAction1.call()
        assert lblRes1.error == null
        LoadBalancerListenerInventory loadBalancerListenerInventory1 = lblRes1.value.inventory

        CreateLoadBalancerAction createLoadBalancerAction2 = new CreateLoadBalancerAction()
        createLoadBalancerAction2.name = "lb-2"
        createLoadBalancerAction2.vipUuid = vipInventory1.uuid
        createLoadBalancerAction2.sessionId = adminSession()
        CreateLoadBalancerAction.Result lbRes2 = createLoadBalancerAction2.call()
        assert lbRes2.error == null
        LoadBalancerInventory loadBalancerInventory2 = lbRes2.value.inventory

        CreateLoadBalancerListenerAction createLoadBalancerListenerAction2 = new CreateLoadBalancerListenerAction()
        createLoadBalancerListenerAction2.loadBalancerUuid = loadBalancerInventory2.uuid
        createLoadBalancerListenerAction2.loadBalancerPort = 24
        createLoadBalancerListenerAction2.instancePort = 24
        createLoadBalancerListenerAction2.name = "ssh"
        createLoadBalancerListenerAction2.protocol = "tcp"
        createLoadBalancerListenerAction2.sessionId = adminSession()
        CreateLoadBalancerListenerAction.Result lblRes2 = createLoadBalancerListenerAction2.call()
        assert lblRes2.error == null

        CreateLoadBalancerListenerAction createLoadBalancerListenerAction3 = new CreateLoadBalancerListenerAction()
        createLoadBalancerListenerAction3.loadBalancerUuid = loadBalancerInventory2.uuid
        createLoadBalancerListenerAction3.loadBalancerPort = 23
        createLoadBalancerListenerAction3.instancePort = 23
        createLoadBalancerListenerAction3.name = "ssh"
        createLoadBalancerListenerAction3.protocol = "tcp"
        createLoadBalancerListenerAction3.sessionId = adminSession()
        CreateLoadBalancerListenerAction.Result lblRes3 = createLoadBalancerListenerAction3.call()
        assert lblRes3.error == null
        LoadBalancerListenerInventory lbl3Inv = lblRes3.value.inventory

        AddVmNicToLoadBalancerAction addAction = new AddVmNicToLoadBalancerAction()
        addAction.listenerUuid = lbl3Inv.uuid
        addAction.vmNicUuids = asList(vm1Nic1.getUuid())
        addAction.sessionId = adminSession()
        AddVmNicToLoadBalancerAction.Result addResult = addAction.call()
        assert addResult.error == null

        AddVmNicToLoadBalancerAction addAction1 = new AddVmNicToLoadBalancerAction()
        addAction1.listenerUuid = loadBalancerListenerInventory1.uuid
        addAction1.vmNicUuids = asList(vm2Nic1.getUuid())
        addAction1.sessionId = adminSession()
        AddVmNicToLoadBalancerAction.Result addResult1 = addAction1.call()
        assert addResult1.error == null

        removeVmNicFromLoadBalancer {
            listenerUuid = lbl3Inv.uuid
            vmNicUuids = asList(vm1Nic1.getUuid())
        }

        /* delete lb-2 , check lb listener attached to lb is deleted cascaded */
        deleteLoadBalancer {
            uuid = loadBalancerInventory2.uuid
        }
        long count = Q.New(LoadBalancerListenerVO.class).select()
                .eq(LoadBalancerListenerVO_.loadBalancerUuid, loadBalancerInventory2.uuid).count()
        assert count == 0
        count =  Q.New(VipNetworkServicesRefVO.class).eq(VipNetworkServicesRefVO_.vipUuid, vipInventory1.uuid)
                .eq(VipNetworkServicesRefVO_.serviceType, LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE_STRING).count()
         assert count == 1

        CreateLoadBalancerAction createLoadBalancerAction3 = new CreateLoadBalancerAction()
        createLoadBalancerAction3.name = "lb-2"
        createLoadBalancerAction3.vipUuid = vipInventory1.uuid
        createLoadBalancerAction3.sessionId = adminSession()
        CreateLoadBalancerAction.Result lbRes3 = createLoadBalancerAction3.call()
        assert lbRes3.error == null

        /* delete vip-1, check lb attached to vip-1 is deleted cascaded */
        deleteVip {
            uuid = vipInventory1.uuid
        }
        count = Q.New(LoadBalancerVO.class).select()
                .eq(LoadBalancerVO_.vipUuid, vipInventory1.uuid).count()
        assert count == 0
    }

    void cleanEnvironment(){
        SQL.New(LoadBalancerListenerVO.class).hardDelete()
    }
}
