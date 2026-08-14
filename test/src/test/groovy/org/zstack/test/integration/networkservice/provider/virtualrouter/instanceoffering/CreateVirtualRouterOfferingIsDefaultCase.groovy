package org.zstack.test.integration.networkservice.provider.virtualrouter.instanceoffering

import org.springframework.http.HttpEntity
import org.zstack.core.db.Q
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.header.vm.VmNicVO
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.lb.LoadBalancerConstants
import org.zstack.network.service.portforwarding.PortForwardingConstant
import org.zstack.network.service.virtualrouter.VirtualRouterOfferingVO
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO
import org.zstack.network.service.virtualrouter.vyos.VyosConstants
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.VirtualRouterOfferingInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.ZoneInventory
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

/**
 * Created by heathhose on 17-6-2.
 */
class CreateVirtualRouterOfferingIsDefaultCase extends SubCase{

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

                    l3Network {
                        name = "pubL3-2"

                        ip {
                            startIp = "11.168.200.10"
                            endIp = "11.168.200.100"
                            netmask = "255.255.255.0"
                            gateway = "11.168.200.1"
                        }
                    }
                }

                attachBackupStorage("sftp")

                eip {
                    name = "eip"
                    useVip("pubL3")
                }

                virtualRouterOffering {
                    name = "vro"
                    memory = SizeUnit.MEGABYTE.toByte(512)
                    cpu = 2
                    useManagementL3Network("pubL3")
                    usePublicL3Network("pubL3")
                    useImage("vr")
                }
            }

        }
    }

    @Override
    void test() {
        env.create {
            testCreateDefaultVirtualRouterOffering()
        }
    }

    void testCreateDefaultVirtualRouterOffering(){
        def pub = env.inventoryByName("pubL3-2") as L3NetworkInventory
        def zone = env.inventoryByName("zone") as ZoneInventory
        def image = env.inventoryByName("vr") as ImageInventory
        def l3 = env.inventoryByName("l3") as L3NetworkInventory
        def offer = env.inventoryByName("instanceOffering") as InstanceOfferingInventory
        def vr = createVirtualRouterOffering {
            name = "vro"
            memorySize = SizeUnit.MEGABYTE.toByte(512)
            cpuNum = 2
            managementNetworkUuid = pub.uuid
            publicNetworkUuid = pub.uuid
            imageUuid = image.uuid
            zoneUuid = zone.uuid
            isDefault = true
        } as VirtualRouterOfferingInventory
        assert dbFindByUuid(vr.uuid, VirtualRouterOfferingVO.class).isDefault()

        //check virtual router use PubL3-2
        KVMAgentCommands.StartVmCmd cmd = null
        Boolean check = false
        env.afterSimulator(KVMConstant.KVM_START_VM_PATH){rsp, HttpEntity<String> entity ->
            cmd = json(entity.getBody(), KVMAgentCommands.StartVmCmd.class)
            for(KVMAgentCommands.NicTO nic :cmd.getNics()){
                if(pub.uuid == dbFindByUuid(nic.getUuid(), VmNicVO.class).l3NetworkUuid){
                    check = true
                }

            }

            return rsp
        }
        createVmInstance {
            name = "vm"
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            instanceOfferingUuid  = offer.uuid
        } as VmInstanceInventory

        List<VirtualRouterVmVO> vrs = Q.New(VirtualRouterVmVO.class).list()
        assert vrs.size() == 1
        assert vrs.get(0).publicNetworkUuid == pub.uuid
        assert check
    }
    @Override
    void clean() {
        env.delete()
    }
}
