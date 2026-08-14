package org.zstack.test.integration.storage.primary.cascade

import org.springframework.http.HttpEntity
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.volume.VolumeVO
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.network.securitygroup.SecurityGroupConstant
import org.zstack.network.service.virtualrouter.VirtualRouterConstant
import org.zstack.sdk.ClusterInventory
import org.zstack.sdk.DiskOfferingInventory
import org.zstack.sdk.GetIpAddressCapacityResult
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VolumeInventory
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

/**
 * Created by AlanJager on 2017/5/10.
 */
class PrimaryStorageDeleteCascadeCase extends SubCase {
    EnvSpec env
    L3NetworkInventory l3

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
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
                    name = "cluster1"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm1"
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

                cluster {
                    name = "cluster2"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm2"
                        managementIp = "127.0.0.2"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("smp")
                    attachL2Network("l2")
                }

                smpPrimaryStorage {
                    name = "smp"
                    url = "/test"
                }

                cluster {
                    name = "cluster3"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm3"
                        managementIp = "127.0.0.3"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("nfs")
                    attachL2Network("l2")
                }

                nfsPrimaryStorage {
                    name = "nfs"
                    url = "localhost:/nfs"
                }

                l2NoVlanNetwork {
                    name = "l2"
                    physicalInterface = "eth0"

                    l3Network {
                        name = "l3"

                        service {
                            provider = VirtualRouterConstant.PROVIDER_TYPE
                            types = [NetworkServiceType.DHCP.toString(), NetworkServiceType.DNS.toString()]
                        }

                        service {
                            provider = SecurityGroupConstant.SECURITY_GROUP_PROVIDER_TYPE
                            types = [SecurityGroupConstant.SECURITY_GROUP_NETWORK_SERVICE_TYPE]
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
                            startIp = "12.16.10.10"
                            endIp = "12.16.10.100"
                            netmask = "255.255.255.0"
                            gateway = "12.16.10.1"
                        }
                    }
                }

                virtualRouterOffering {
                    name = "vr"
                    memory = SizeUnit.MEGABYTE.toByte(512)
                    cpu = 2
                    useManagementL3Network("pubL3")
                    usePublicL3Network("pubL3")
                    useImage("vr")
                }

                attachBackupStorage("sftp")
            }

            vm {
                name = "vm1"
                useInstanceOffering("instanceOffering")
                useImage("image1")
                useL3Networks("l3")
                useRootDiskOffering("diskOffering")
                useHost("kvm1")
            }

            vm {
                name = "vm2"
                useInstanceOffering("instanceOffering")
                useImage("image1")
                useL3Networks("l3")
                useRootDiskOffering("diskOffering")
                useHost("kvm2")
            }

            vm {
                name = "vm3"
                useInstanceOffering("instanceOffering")
                useImage("image1")
                useL3Networks("l3")
                useRootDiskOffering("diskOffering")
                useHost("kvm3")
            }
        }
    }

    @Override
    void test() {
        env.create {
            l3 = env.inventoryByName("l3")
            GetIpAddressCapacityResult capacityResult = getIpAddressCapacity {
                l3NetworkUuids = [l3.uuid]
            }

            testPrimaryStorageDeleteCascadeWhenUseLocalStorage()
            testPrimaryStorageDeleteCascadeWhenUseSMP()
            testPrimaryStorageDeleteCascadeWhenUseNfs()

            retryInSecs {
                GetIpAddressCapacityResult result = getIpAddressCapacity {
                    l3NetworkUuids = [l3.uuid]
                }
                assert capacityResult.availableCapacity + 4 == result.availableCapacity
            }
        }
    }

    void testPrimaryStorageDeleteCascadeWhenUseLocalStorage() {
        DiskOfferingInventory diskOffering = env.inventoryByName("diskOffering")
        VmInstanceInventory vm = env.inventoryByName("vm1")
        PrimaryStorageInventory ps = env.inventoryByName("local")
        ClusterInventory cluster = env.inventoryByName("cluster1")

        VolumeInventory volume = createDataVolume {
            name = "data1"
            diskOfferingUuid = diskOffering.uuid
        }

        attachDataVolumeToVm {
            vmInstanceUuid = vm.getUuid()
            volumeUuid = volume.uuid
        }

        // detach cascade will set vm to stopped
        detachPrimaryStorageFromCluster {
            primaryStorageUuid = ps.getUuid()
            clusterUuid = cluster.getUuid()
        }
        VmInstanceVO vo
        retryInSecs {
            vo = dbFindByUuid(vm.getUuid(), VmInstanceVO.class)
            assert vo.state == VmInstanceState.Stopped
        }

        KVMAgentCommands.DestroyVmCmd destroyCmd = null
        env.afterSimulator(KVMConstant.KVM_DESTROY_VM_PATH) { KVMAgentCommands.DestroyVmResponse rsp, HttpEntity<String> e ->
            destroyCmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.DestroyVmCmd.class)
            return rsp
        }

        // delete primary storage cascade will destroy vm and data volume using Keep volume strategy
        deletePrimaryStorage {
            uuid = ps.getUuid()
        }
        VolumeVO volVO
        retryInSecs {
            vo = dbFindByUuid(vm.getUuid(), VmInstanceVO.class)
            volVO = dbFindByUuid(volume.getUuid(), VolumeVO.class)

            return {
                assert vo == null
                assert volVO == null
            }
        }
        assert destroyCmd == null
    }

    void testPrimaryStorageDeleteCascadeWhenUseSMP() {
        DiskOfferingInventory diskOffering = env.inventoryByName("diskOffering")
        VmInstanceInventory vm = env.inventoryByName("vm2")
        PrimaryStorageInventory ps = env.inventoryByName("smp")
        ClusterInventory cluster = env.inventoryByName("cluster2")

        VolumeInventory volume = createDataVolume {
            name = "data1"
            diskOfferingUuid = diskOffering.uuid
        }

        attachDataVolumeToVm {
            vmInstanceUuid = vm.getUuid()
            volumeUuid = volume.uuid
        }

        // detach cascade will set vm to stopped
        detachPrimaryStorageFromCluster {
            primaryStorageUuid = ps.getUuid()
            clusterUuid = cluster.getUuid()
        }
        VmInstanceVO vo
        retryInSecs {
            vo = dbFindByUuid(vm.getUuid(), VmInstanceVO.class)
            assert vo.state == VmInstanceState.Stopped
        }

        KVMAgentCommands.DestroyVmCmd destroyCmd = null
        env.afterSimulator(KVMConstant.KVM_DESTROY_VM_PATH) { KVMAgentCommands.DestroyVmResponse rsp, HttpEntity<String> e ->
            destroyCmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.DestroyVmCmd.class)
            return rsp
        }
        // delete primary storage cascade will destroy vm and data volume using delay strategy
        deletePrimaryStorage {
            uuid = ps.getUuid()
        }
        VolumeVO volVO
        retryInSecs {
            vo = dbFindByUuid(vm.getUuid(), VmInstanceVO.class)
            volVO = dbFindByUuid(volume.getUuid(), VolumeVO.class)

            return {
                assert vo == null
                assert volVO == null
            }
        }
        assert destroyCmd == null
    }

    void testPrimaryStorageDeleteCascadeWhenUseNfs() {
        DiskOfferingInventory diskOffering = env.inventoryByName("diskOffering")
        VmInstanceInventory vm = env.inventoryByName("vm3")
        PrimaryStorageInventory ps = env.inventoryByName("nfs")
        ClusterInventory cluster = env.inventoryByName("cluster3")

        VolumeInventory volume = createDataVolume {
            name = "data1"
            diskOfferingUuid = diskOffering.uuid
        }

        attachDataVolumeToVm {
            vmInstanceUuid = vm.getUuid()
            volumeUuid = volume.uuid
        }

        // detach cascade will set vm to stopped
        detachPrimaryStorageFromCluster {
            primaryStorageUuid = ps.getUuid()
            clusterUuid = cluster.getUuid()
        }
        VmInstanceVO vo
        retryInSecs {
            vo = dbFindByUuid(vm.getUuid(), VmInstanceVO.class)
            assert vo.state == VmInstanceState.Stopped
        }

        KVMAgentCommands.DestroyVmCmd destroyCmd = null
        env.afterSimulator(KVMConstant.KVM_DESTROY_VM_PATH) { KVMAgentCommands.DestroyVmResponse rsp, HttpEntity<String> e ->
            destroyCmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.DestroyVmCmd.class)
            return rsp
        }
        // delete primary storage cascade will destroy vm and data volume using delay strategy
        deletePrimaryStorage {
            uuid = ps.getUuid()
        }
        VolumeVO volVO
        retryInSecs {
            vo = dbFindByUuid(vm.getUuid(), VmInstanceVO.class)
            volVO = dbFindByUuid(volume.getUuid(), VolumeVO.class)
            assert vo == null
            assert volVO == null
        }
        assert destroyCmd == null
    }
}
