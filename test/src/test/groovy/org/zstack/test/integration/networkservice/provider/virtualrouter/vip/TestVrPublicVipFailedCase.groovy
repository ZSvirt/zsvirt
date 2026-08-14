package org.zstack.test.integration.networkservice.provider.virtualrouter.vip

import org.zstack.core.db.Q
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.flat.FlatNetworkServiceConstant
import org.zstack.network.service.lb.LoadBalancerConstants
import org.zstack.network.service.portforwarding.PortForwardingConstant
import org.zstack.network.service.userdata.UserdataConstant
import org.zstack.network.service.vip.VipVO
import org.zstack.network.service.vip.VipVO_
import org.zstack.network.service.virtualrouter.VirtualRouterCommands
import org.zstack.network.service.virtualrouter.VirtualRouterConstant
import org.zstack.network.service.virtualrouter.vyos.VyosConstants
import org.zstack.sdk.*
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

import java.util.stream.Collectors

/**
 * Created by shixin on 2018-03-13.
 */
class TestVrPublicVipFailedCase extends SubCase{
    EnvSpec env
    VmInstanceInventory vm

    @Override
    void setup() {
        useSpring(NetworkServiceProviderTest.springSpec)
    }

    @Override
    void environment() {
        env = env {
            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.MEGABYTE.toByte(512)
                cpu = 1
            }

            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                username = "root"
                password = "password"
                hostname = "localhost"
                image {
                    name = "vr"
                    url = "http://zstack.org/download/vr.qcow2"
                }
                image {
                    name = "image"
                    url = "http://zstack.org/download/test.qcow2"
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
                        category = "Public"

                        service {
                            provider = FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING
                            types = [NetworkServiceType.DHCP.toString(),
                                     NetworkServiceType.HostRoute.toString(),
                                     UserdataConstant.USERDATA_TYPE_STRING]
                        }

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
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void test() {
        env.create {
            TestVrPublicVipFail()
            TestCreateVipSameToVmIpFail()
        }
    }

    void TestVrPublicVipFail(){
        ImageInventory image = env.inventoryByName("image")
        InstanceOfferingInventory offer = env.inventoryByName("instanceOffering")
        L3NetworkInventory l3Inv = env.inventoryByName("l3")
        L3NetworkInventory pubL3 = env.inventoryByName("pubL3")

        env.simulator(VirtualRouterConstant.VR_CREATE_VIP) {
            VirtualRouterCommands.CreateVipRsp rsp = new VirtualRouterCommands.CreateVipRsp()
            rsp.success = false
            rsp.setError("test")
            return rsp
        }

        expect(AssertionError.class) {
            createVmInstance {
                name = "vm-1"
                instanceOfferingUuid = offer.uuid
                imageUuid = image.uuid
                l3NetworkUuids = [l3Inv.uuid]
            }
        }

        assert Q.New(VipVO.class).count() == 0
        env.simulator(VirtualRouterConstant.VR_CREATE_VIP) {
            VirtualRouterCommands.CreateVipRsp rsp = new VirtualRouterCommands.CreateVipRsp()
            return rsp
        }

        createVmInstance {
            name = "test-vm"
            instanceOfferingUuid = offer.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3Inv.uuid]
        }

        VirtualRouterVmInventory vr = queryVirtualRouterVm {}[0]
        List<VmNicInventory> nics = vr.getVmNics()
        VmNicInventory pubNic = nics.stream().filter({nic -> nic.l3NetworkUuid == pubL3.uuid}).collect(Collectors.toList())[0]
        assert Q.New(VipVO.class).eq(VipVO_.ip, pubNic.ip).exists

        env.simulator(VirtualRouterConstant.VR_CREATE_VIP) {
            VirtualRouterCommands.CreateVipRsp rsp = new VirtualRouterCommands.CreateVipRsp()
            rsp.success = false
            rsp.setError("test")
            return rsp
        }

        stopVmInstance {
            uuid = vr.uuid
        }
        expect (AssertionError.class) {
            startVmInstance {
                uuid = vr.uuid
            }
        }
        assert Q.New(VipVO.class).eq(VipVO_.ip, pubNic.ip).exists

        env.simulator(VirtualRouterConstant.VR_CREATE_VIP) {
            VirtualRouterCommands.CreateVipRsp rsp = new VirtualRouterCommands.CreateVipRsp()
            return rsp
        }
        startVmInstance {
            uuid = vr.uuid
        }
        assert Q.New(VipVO.class).eq(VipVO_.ip, pubNic.ip).exists
        assert Q.New(VipVO.class).count() == 1

        destroyVmInstance {
            uuid = vr.uuid
        }
        assert !Q.New(VipVO.class).eq(VipVO_.ip, pubNic.ip).exists
        assert Q.New(VipVO.class).count() == 0
    }

    void TestCreateVipSameToVmIpFail(){
        ImageInventory image = env.inventoryByName("image")
        InstanceOfferingInventory offer = env.inventoryByName("instanceOffering")
        L3NetworkInventory pubL3 = env.inventoryByName("pubL3")

        vm = createVmInstance {
            name = "pub-vm"
            instanceOfferingUuid = offer.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [pubL3.uuid]
        }
        VmNicInventory nic = vm.getVmNics().get(0)

        expect (AssertionError.class) {
            createVip {
                name = "vip"
                l3NetworkUuid = pubL3.uuid
                requiredIp = nic.ip
            }
        }
    }
}
