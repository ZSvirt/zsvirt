package org.zstack.test.integration.storage.primary.local_nfs

import org.zstack.sdk.*
import org.zstack.storage.primary.local.LocalStorageSystemTags
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import static org.zstack.utils.CollectionDSL.map
import static org.zstack.utils.CollectionDSL.e

/**
 * Created by lining on 2017-10-13.
 */
class GetAttachableVmsForDataVolumeCase extends SubCase{
    EnvSpec env

    VmInstanceInventory vmRootVolumeIsLocalOnHost1
    VmInstanceInventory vmRootVolumeIsLocalOnHost2
    VmInstanceInventory vmRootVolumeIsNfsOnHost1
    VmInstanceInventory vmRootVolumeIsNfsOnHost2

    VolumeInventory localDataVolumeOnHost1
    VolumeInventory localDataVolumeOnHost2
    VolumeInventory nfsDataVolume


    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
    }

    @Override
    void environment() {
        env = env{
            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(1)
                cpu = 1
            }

            diskOffering {
                name = "diskOffering"
                diskSize = SizeUnit.GIGABYTE.toByte(1)
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
                        totalCpu = 88
                        totalMem = SizeUnit.GIGABYTE.toByte(100)
                    }

                    kvm {
                        name = "kvm2"
                        managementIp = "127.0.0.2"
                        username = "root"
                        password = "password"
                        totalCpu = 88
                        totalMem = SizeUnit.GIGABYTE.toByte(100)
                    }

                    attachPrimaryStorage("local")
                    attachPrimaryStorage("nfs")
                    attachL2Network("l2")
                }

                localPrimaryStorage {
                    name = "local"
                    url = "/local_ps"
                }

                nfsPrimaryStorage {
                    name = "nfs"
                    url = "127.20.0.1:/nfs_root"
                }

                l2NoVlanNetwork {
                    name = "l2"
                    physicalInterface = "eth0"

                    l3Network {
                        name = "l3"

                        ip {
                            startIp = "12.16.10.10"
                            endIp = "12.16.10.100"
                            netmask = "255.255.255.0"
                            gateway = "12.16.10.1"
                        }
                    }
                }

                attachBackupStorage("sftp")
            }
        }
    }

    @Override
    void test() {
        env.create {
            getAttachableRunningVms()
            getAttachableStoppedVms()
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    void getAttachableRunningVms(){
        PrimaryStorageInventory nfs = env.inventoryByName("nfs") as PrimaryStorageInventory
        PrimaryStorageInventory local = env.inventoryByName("local") as PrimaryStorageInventory
        InstanceOfferingInventory instanceOffering = env.inventoryByName("instanceOffering") as InstanceOfferingInventory
        DiskOfferingInventory diskOffering = env.inventoryByName("diskOffering") as DiskOfferingInventory
        ImageInventory image = env.inventoryByName("image") as ImageInventory
        L3NetworkInventory l3 = env.inventoryByName("l3") as L3NetworkInventory
        HostInventory host1 = env.inventoryByName("kvm")
        HostInventory host2 = env.inventoryByName("kvm2")

        // local root volume
             // vm on host1
             // vm on host2
        vmRootVolumeIsLocalOnHost1 = createVmInstance {
            name = "vm"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            hostUuid = host1.uuid
            primaryStorageUuidForRootVolume = local.uuid
        }

        vmRootVolumeIsLocalOnHost2 = createVmInstance {
            name = "vm"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            hostUuid = host2.uuid
            primaryStorageUuidForRootVolume = local.uuid
        }


        // nfs root volume
            // vm on host1
            // vm on host2
        vmRootVolumeIsNfsOnHost1 = createVmInstance {
            name = "vm"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            hostUuid = host1.uuid
            primaryStorageUuidForRootVolume = nfs.uuid
        }

        vmRootVolumeIsNfsOnHost2 = createVmInstance {
            name = "vm"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            hostUuid = host2.uuid
            primaryStorageUuidForRootVolume = nfs.uuid
        }

        // local data volume
            // data volume in host1
            // data volume in host2
        String localStorageSystemTag = LocalStorageSystemTags.DEST_HOST_FOR_CREATING_DATA_VOLUME.instantiateTag(
                map(e(LocalStorageSystemTags.DEST_HOST_FOR_CREATING_DATA_VOLUME_TOKEN, host1.uuid))
        )
        localDataVolumeOnHost1 = createDataVolume {
            name = "data"
            diskOfferingUuid = diskOffering.uuid
            primaryStorageUuid = local.uuid
            systemTags = [localStorageSystemTag]
        }
        attachDataVolumeToVm {
            vmInstanceUuid = vmRootVolumeIsLocalOnHost1.uuid
            volumeUuid = localDataVolumeOnHost1.uuid
        }
        detachDataVolumeFromVm {
            vmUuid = vmRootVolumeIsLocalOnHost1.uuid
            uuid = localDataVolumeOnHost1.uuid
        }

        localStorageSystemTag = LocalStorageSystemTags.DEST_HOST_FOR_CREATING_DATA_VOLUME.instantiateTag(
                map(e(LocalStorageSystemTags.DEST_HOST_FOR_CREATING_DATA_VOLUME_TOKEN, host2.uuid))
        )
        localDataVolumeOnHost2 = createDataVolume {
            name = "data"
            diskOfferingUuid = diskOffering.uuid
            primaryStorageUuid = local.uuid
            systemTags = [localStorageSystemTag]
        }
        attachDataVolumeToVm {
            vmInstanceUuid = vmRootVolumeIsLocalOnHost2.uuid
            volumeUuid = localDataVolumeOnHost2.uuid
        }
        detachDataVolumeFromVm {
            vmUuid = vmRootVolumeIsLocalOnHost2.uuid
            uuid = localDataVolumeOnHost2.uuid
        }

        // nfs data volume
        nfsDataVolume = createDataVolume {
            name = "data"
            diskOfferingUuid = diskOffering.uuid
            primaryStorageUuid = nfs.uuid
        }
        attachDataVolumeToVm {
            vmInstanceUuid = vmRootVolumeIsNfsOnHost1.uuid
            volumeUuid = nfsDataVolume.uuid
        }
        detachDataVolumeFromVm {
            vmUuid = vmRootVolumeIsNfsOnHost1.uuid
            uuid = nfsDataVolume.uuid
        }

        // test begin

        List<VmInstanceInventory> result = getDataVolumeAttachableVm {
            volumeUuid = localDataVolumeOnHost1.uuid
        }
        check(result, [vmRootVolumeIsLocalOnHost1.uuid, vmRootVolumeIsNfsOnHost1.uuid])


        result = getDataVolumeAttachableVm {
            volumeUuid = localDataVolumeOnHost2.uuid
        }
        check(result, [vmRootVolumeIsLocalOnHost2.uuid, vmRootVolumeIsNfsOnHost2.uuid])


        result = getDataVolumeAttachableVm {
            volumeUuid = nfsDataVolume.uuid
        }
        check(result, [vmRootVolumeIsLocalOnHost1.uuid, vmRootVolumeIsNfsOnHost1.uuid, vmRootVolumeIsLocalOnHost2.uuid, vmRootVolumeIsNfsOnHost2.uuid])

    }

    void getAttachableStoppedVms(){
        stopVmInstance {
            uuid = vmRootVolumeIsLocalOnHost1.uuid
        }
        stopVmInstance {
            uuid = vmRootVolumeIsLocalOnHost2.uuid
        }
        stopVmInstance {
            uuid = vmRootVolumeIsNfsOnHost1.uuid
        }
        stopVmInstance {
            uuid = vmRootVolumeIsNfsOnHost2.uuid
        }

        List<VmInstanceInventory> result = getDataVolumeAttachableVm {
            volumeUuid = localDataVolumeOnHost1.uuid
        }
        check(result, [vmRootVolumeIsLocalOnHost1.uuid, vmRootVolumeIsNfsOnHost1.uuid, vmRootVolumeIsNfsOnHost2.uuid])


        result = getDataVolumeAttachableVm {
            volumeUuid = localDataVolumeOnHost2.uuid
        }
        check(result, [vmRootVolumeIsLocalOnHost2.uuid, vmRootVolumeIsNfsOnHost1.uuid, vmRootVolumeIsNfsOnHost2.uuid])


        result = getDataVolumeAttachableVm {
            volumeUuid = nfsDataVolume.uuid
        }
        check(result, [vmRootVolumeIsLocalOnHost1.uuid, vmRootVolumeIsNfsOnHost1.uuid, vmRootVolumeIsLocalOnHost2.uuid, vmRootVolumeIsNfsOnHost2.uuid])

    }

    private check(List<VmInstanceInventory> result, List<String> expect){
        List<String> resultList = []
        for(VmInstanceInventory vm : result){
            resultList.add(vm.uuid)
        }

        assert resultList.size() == expect.size()
        assert resultList.containsAll(expect)
    }

}
