package org.zstack.test.integration.networkservice.provider.flat.eip

import org.springframework.http.HttpEntity
import org.zstack.header.host.HostStatus
import org.zstack.header.host.HostVO
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.eip.EipVO
import org.zstack.network.service.flat.FlatEipBackend
import org.zstack.network.service.flat.FlatNetworkServiceConstant
import org.zstack.network.service.lb.LoadBalancerConstants
import org.zstack.network.service.userdata.UserdataConstant
import org.zstack.network.service.virtualrouter.vyos.VyosConstants
import org.zstack.sdk.*
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
/**
 * Created by heathhose on 17-5-15.
 */
class OperateEipCase extends SubCase {
    EipInventory eip
    VmInstanceInventory vm
    L3NetworkInventory pub_l3
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
                    attachL2Network("l2-1")
                }

                cluster {
                    name = "cluster2"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm2"
                        managementIp = "127.0.0.2"
                        username = "root"
                        password = "password"
                    }

                    attachL2Network("l2vlan")
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
                            provider = FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING
                            types = [NetworkServiceType.DHCP.toString(), EipConstant.EIP_NETWORK_SERVICE_TYPE, UserdataConstant.USERDATA_TYPE_STRING]
                        }

                        service {
                            provider = VyosConstants.VYOS_ROUTER_PROVIDER_TYPE
                            types = [LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE_STRING]
                        }

                        ip {
                            startIp = "192.168.100.10"
                            endIp = "192.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.100.1"
                        }
                    }

                    l3Network {
                        name = "l3-2"

                        service {
                            provider = FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING
                            types = [NetworkServiceType.DHCP.toString(), EipConstant.EIP_NETWORK_SERVICE_TYPE, UserdataConstant.USERDATA_TYPE_STRING]
                        }

                        ip {
                            startIp = "11.168.0.3"
                            endIp = "11.168.1.200"
                            netmask = "255.255.0.0"
                            gateway = "11.168.0.1"
                        }
                    }
                }

                l2NoVlanNetwork {
                    name = "l2-1"
                    physicalInterface = "eth1"

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

                l2VlanNetwork {
                    name = "l2vlan"
                    physicalInterface = "eth0"
                    vlan = 1001

                    l3Network {
                        name = "pubL3_2"

                        ip {
                            startIp = "12.168.100.10"
                            endIp = "12.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "12.168.100.1"
                        }
                    }
                }

                attachBackupStorage("sftp")

                eip {
                    name = "eip"

                    useVip("pubL3")
                }

                eip {
                    name = "eip-2"

                    useVip("pubL3")
                }

                eip {
                    name = "eip-3"
                    useVip("pubL3_2")
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

            vm {
                name = "vm"
                useImage("image")
                useL3Networks("l3")
                useInstanceOffering("instanceOffering")
            }

            vm {
                name = "vm-2"
                useImage("image")
                useL3Networks("l3-2")
                useInstanceOffering("instanceOffering")
            }
        }
    }

    @Override
    void test() {
        env.create {
            eip = env.inventoryByName("eip") as EipInventory
            vm = env.inventoryByName("vm") as VmInstanceInventory
            pub_l3 = env.inventoryByName("pubL3") as L3NetworkInventory

            testAttachVmNicToEip()
            testDetachEipWhenVmStopped()
            testReconnectHostBatchApplyEips()
            testDeleteEip()
            testAttachEipToOverlapCidrVmNic()
            testAttachEipDifferentClusterAttached()
        }
    }

    void testAttachEipToOverlapCidrVmNic() {
        def eip_2 = env.inventoryByName("eip-2") as EipInventory
        def vm_2 = env.inventoryByName("vm-2") as VmInstanceInventory

        expect(AssertionError) {
            attachEip {
                eipUuid = eip_2.uuid
                vmNicUuid = vm_2.getVmNics().get(0).getUuid()
            }
        }
    }

    void testAttachVmNicToEip() {
        FlatEipBackend.ApplyEipCmd cmd = new FlatEipBackend.ApplyEipCmd()
        env.afterSimulator(FlatEipBackend.APPLY_EIP_PATH) { rsp, HttpEntity<String> entity ->
            cmd = json(entity.getBody(), FlatEipBackend.ApplyEipCmd.class)
            return rsp

        }
        String vipUuid = eip.vipUuid
        GetEipAttachableVmNicsAction getEipAttachableVmNicsAction = new GetEipAttachableVmNicsAction()
        getEipAttachableVmNicsAction.eipUuid = eip.uuid
        getEipAttachableVmNicsAction.vipUuid = vipUuid
        getEipAttachableVmNicsAction.sessionId = adminSession()
        GetEipAttachableVmNicsAction.Result res = getEipAttachableVmNicsAction.call()
        assert res.error == null
        assert res.value.inventories != null
        assert res.value.inventories.size() == 2

        attachEip {
            eipUuid = eip.uuid
            vmNicUuid = vm.getVmNics().get(0).getUuid()
        }

        assert cmd.eip.eipUuid == eip.uuid
        assert dbFindByUuid(eip.uuid, EipVO.class).guestIp == vm.getVmNics().get(0).getIp()

        res = getEipAttachableVmNicsAction.call()
        assert res.error == null
        assert res.value.inventories != null
        assert res.value.inventories.size() == 0
    }

    void testDetachEipWhenVmStopped(){
        stopVmInstance {
            uuid = vm.uuid
        }

        detachEip {
            uuid = eip.uuid
        }

        startVmInstance {
            uuid = vm.uuid
        }

        attachEip {
            eipUuid = eip.uuid
            vmNicUuid = vm.getVmNics().get(0).getUuid()
        }
    }


    void testDeleteEip() {
        FlatEipBackend.DeleteEipCmd cmd = new FlatEipBackend.DeleteEipCmd()
        env.afterSimulator(FlatEipBackend.DELETE_EIP_PATH) { rsp, HttpEntity<String> entity ->
            cmd = json(entity.getBody(), FlatEipBackend.DeleteEipCmd.class)
            return rsp
        }

        deleteEip {
            uuid = eip.uuid
        }

        assert cmd.eip.eipUuid == eip.uuid
        assert dbFindByUuid(eip.uuid, EipVO.class) == null
    }

    void testReconnectHostBatchApplyEips() {
        def host = env.inventoryByName("kvm") as HostInventory
        FlatEipBackend.BatchApplyEipCmd cmd = new FlatEipBackend.BatchApplyEipCmd()

        env.afterSimulator(FlatEipBackend.BATCH_APPLY_EIP_PATH) { rsp, HttpEntity<String> entity ->
            cmd = json(entity.getBody(), FlatEipBackend.BatchApplyEipCmd)
            return rsp
        }
        reconnectHost {
            uuid = host.uuid
        }

        assert cmd.eips.size() == 1
        assert cmd.eips.get(0).eipUuid == eip.uuid
        retryInSecs() {
            assert dbFindByUuid(host.uuid, HostVO.class).getStatus() == HostStatus.Connected
        }
    }

    void testAttachEipDifferentClusterAttached(){
        def eip_3 = env.inventoryByName("eip-3") as EipInventory
        def vm = env.inventoryByName("vm") as VmInstanceInventory
        String vipUuid = eip_3.vipUuid

        def a = new AttachEipAction()
        a.eipUuid = eip_3.uuid
        a.vmNicUuid = vm.vmNics.get(0).uuid
        a.sessionId = currentEnvSpec.session.uuid
        assert a.call().error != null

        deleteEip {
            uuid = eip_3.uuid
        }

        a = new CreateEipAction()
        a.name = "test"
        a.vmNicUuid = vm.vmNics.get(0).uuid
        a.vipUuid = vipUuid
        a.sessionId = currentEnvSpec.session.uuid
        assert a.call().error != null
    }

    @Override
    void clean() {
        env.delete()
    }
}
