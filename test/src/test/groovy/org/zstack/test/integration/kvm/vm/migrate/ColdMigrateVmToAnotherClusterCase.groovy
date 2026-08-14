package org.zstack.test.integration.kvm.vm.migrate

import org.zstack.header.host.HostVO
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.network.securitygroup.SecurityGroupConstant
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.flat.FlatNetworkServiceConstant
import org.zstack.network.service.userdata.UserdataConstant
import org.zstack.network.service.virtualrouter.VirtualRouterConstant
import org.zstack.sdk.ClusterInventory
import org.zstack.sdk.HostInventory
import org.zstack.sdk.LocalStorageGetVolumeMigratableHostsAction
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VolumeInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

/**
 * Created by heathhose on 17-4-27.
 */
class ColdMigrateVmToAnotherClusterCase extends SubCase{
    EnvSpec env

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = env {
            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(8)
                cpu = 4
            }

            diskOffering {
                name = "diskOffering"
                diskSize = SizeUnit.GIGABYTE.toByte(20)
            }

            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                username = "root"
                password = "password"
                hostname = "localhost"

                image {
                    name = "image1"
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
                    name = "cluster1-1"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm1"
                        managementIp = "127.0.0.1"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("local")
                    attachL2Network("l2-1")
                }

                cluster {
                    name = "cluster1-2"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm2"
                        managementIp = "127.0.0.2"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("local")
                    attachL2Network("l2-1")
                }

                cluster {
                    name = "cluster2-1"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm3"
                        managementIp = "127.0.0.3"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("local")
                    attachL2Network("l2-2")
                }

                localPrimaryStorage {
                    name = "local"
                    url = "/local_ps"
                }

                l2NoVlanNetwork {
                    name = "l2-1"
                    physicalInterface = "eth0"

                    l3Network {
                        name = "l3-1"

                        service {
                            provider = FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING
                            types = [NetworkServiceType.DHCP.toString(), EipConstant.EIP_NETWORK_SERVICE_TYPE, UserdataConstant.USERDATA_TYPE_STRING]
                        }

                        ip {
                            startIp = "192.168.100.10"
                            endIp = "192.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.100.1"
                        }
                    }
                }
                l2NoVlanNetwork {
                    name = "l2-2"
                    physicalInterface = "eth0"

                    l3Network {
                        name = "l3-2"

                        service {
                            provider = FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING
                            types = [NetworkServiceType.DHCP.toString(), EipConstant.EIP_NETWORK_SERVICE_TYPE, UserdataConstant.USERDATA_TYPE_STRING]
                        }

                        ip {
                            startIp = "192.168.200.10"
                            endIp = "192.168.200.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.200.1"
                        }
                    }
                }

                attachBackupStorage("sftp")
            }

            vm {
                name = "vm"
                useInstanceOffering("instanceOffering")
                useImage("image1")
                useL3Networks("l3-1")
                useCluster("cluster1-1")
            }
        }
    }

    @Override
    void test() {
        env.create {
            testGetVolumeMigratableHosts()
            testMigrateVmToCluster()
            testMigrateVmToClusterNetworkNotSatisfactory()
        }
    }

    void testGetVolumeMigratableHosts(){
        def vmi = env.inventoryByName("vm") as VmInstanceInventory
        def list =  localStorageGetVolumeMigratableHosts {
            volumeUuid = vmi.rootVolumeUuid
        } as List<HostInventory>
        for(HostInventory host:list){
            if(host.name.equals("kvm3")){
                assert false
            }
        }
    }

    void testMigrateVmToCluster(){
        def vmi = env.inventoryByName("vm") as VmInstanceInventory
        def hosti = env.inventoryByName("kvm2") as HostInventory
        def clusteri = env.inventoryByName("cluster1-2") as ClusterInventory
        stopVmInstance {
            uuid = vmi.uuid
        }
        retryInSecs{
            assert dbFindByUuid(vmi.uuid,VmInstanceVO.class).state == VmInstanceState.Stopped
        }

        localStorageMigrateVolume {
            volumeUuid = vmi.rootVolumeUuid
            destHostUuid = hosti.uuid
        }
        retryInSecs{
            assert dbFindByUuid(vmi.uuid,VmInstanceVO.class).clusterUuid == clusteri.uuid
        }
    }

    void testMigrateVmToClusterNetworkNotSatisfactory(){
        def vmi = env.inventoryByName("vm") as VmInstanceInventory
        def hosti = env.inventoryByName("kvm3") as HostInventory
        def clusteri = env.inventoryByName("cluster2-1") as ClusterInventory
        expect(AssertionError.class){
            localStorageMigrateVolume {
                volumeUuid = vmi.rootVolumeUuid
                destHostUuid = hosti.uuid
            }
        }
        assert dbFindByUuid(vmi.uuid,VmInstanceVO.class).clusterUuid != clusteri.uuid
    }

    @Override
    void clean() {
        env.delete()
    }
}
