package org.zstack.test.integration.storage.primary.local_nfs

import org.zstack.sdk.*
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

/**
 * Created by lining on 2017-06-23.
 */
class CreateVmAssignNfsPsCase extends SubCase{
    EnvSpec env

    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
    }

    @Override
    void environment() {
        env = env {
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

                    attachPrimaryStorage("local")
                    attachPrimaryStorage("local2")
                    attachL2Network("l2")
                }

                localPrimaryStorage {
                    name = "local"
                    url = "/local_ps"
                }

                localPrimaryStorage {
                    name = "local2"
                    url = "/local_ps2"
                }

                nfsPrimaryStorage {
                    name = "nfs"
                    url = "172.20.0.1:/nfs_root"
                }

                nfsPrimaryStorage {
                    name = "nfs2"
                    url = "172.20.0.2:/nfs_root"
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
            localAndLocal()
            localAndNfs()
            NfsAndNfs()
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    void localAndLocal(){
        PrimaryStorageInventory local = env.inventoryByName("local") as PrimaryStorageInventory
        PrimaryStorageInventory local2 = env.inventoryByName("local2") as PrimaryStorageInventory
        DiskOfferingInventory diskOffering = env.inventoryByName("diskOffering") as DiskOfferingInventory
        ImageInventory image = env.inventoryByName("image") as ImageInventory
        L3NetworkInventory l3 = env.inventoryByName("l3") as L3NetworkInventory

        retryInSecs{
            GetCandidatePrimaryStoragesForCreatingVmAction getAction = new GetCandidatePrimaryStoragesForCreatingVmAction(
                    l3NetworkUuids : [l3.uuid],
                    imageUuid: image.uuid,
                    dataDiskOfferingUuids: [diskOffering.uuid],
                    sessionId: adminSession()
            )
            GetCandidatePrimaryStoragesForCreatingVmResult getResult = getAction.call().value
            List<PrimaryStorageInventory> rootVolumePrimaryStorages = getResult.rootVolumePrimaryStorages
            List<PrimaryStorageInventory> dataVolumePrimaryStorages = getResult.dataVolumePrimaryStorages.get(diskOffering.uuid)
            assert rootVolumePrimaryStorages.size() == 2
            assert [rootVolumePrimaryStorages[0].uuid, rootVolumePrimaryStorages[1].uuid].containsAll([local.uuid, local2.uuid])
            assert dataVolumePrimaryStorages.size() == 2
            assert [dataVolumePrimaryStorages[0].uuid, dataVolumePrimaryStorages[1].uuid].containsAll([local.uuid, local2.uuid])
        }

        logger.info("Test 101: not assign ps")
        createVmInstance {
            name = "vm101"
            cpuNum = 1
            memorySize = gb(1)
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            diskAOs = [
                [
                    "boot" : true,
                ],
                [
                    "size" : gb(1),
                ],
            ]
        }

        logger.info("Test 102: assign root volume ps")
        def vm = createVmInstance {
            name = "vm102"
            cpuNum = 1
            memorySize = gb(1)
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            diskAOs = [
                [
                    "boot" : true,
                    "primaryStorageUuid" : local.uuid,
                ],
                [
                    "size" : gb(1),
                ],
            ]
        } as VmInstanceInventory
        checkVmRootDiskPs(vm, local.uuid)

        logger.info("Test 103: assign data volume ps")
        vm = createVmInstance {
            name = "vm103"
            cpuNum = 1
            memorySize = gb(1)
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            diskAOs = [
                [
                    "boot" : true,
                ],
                [
                    "size" : gb(1),
                    "primaryStorageUuid" : local.uuid,
                ],
            ]
        } as VmInstanceInventory
        checkVmDataDiskPs(vm, local.uuid)

        logger.info("Test 104: assign data, root volume ps")
        vm = createVmInstance {
            name = "vm104"
            cpuNum = 1
            memorySize = gb(1)
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            diskAOs = [
                [
                    "boot" : true,
                    "primaryStorageUuid" : local.uuid,
                ],
                [
                    "size" : gb(1),
                    "primaryStorageUuid" : local2.uuid,
                ],
            ]
        } as VmInstanceInventory
        checkVmDataDiskPs(vm, local2.uuid)
        checkVmRootDiskPs(vm, local.uuid)
    }

    void localAndNfs(){
        ClusterInventory cluster = env.inventoryByName("cluster") as ClusterInventory
        PrimaryStorageInventory nfs = env.inventoryByName("nfs") as PrimaryStorageInventory
        PrimaryStorageInventory local = env.inventoryByName("local") as PrimaryStorageInventory
        PrimaryStorageInventory local2 = env.inventoryByName("local2") as PrimaryStorageInventory
        DiskOfferingInventory diskOffering = env.inventoryByName("diskOffering") as DiskOfferingInventory
        ImageInventory image = env.inventoryByName("image") as ImageInventory
        L3NetworkInventory l3 = env.inventoryByName("l3") as L3NetworkInventory

        detachPrimaryStorageFromCluster {
            clusterUuid = cluster.uuid
            primaryStorageUuid = local2.uuid
        }

        attachPrimaryStorageToCluster {
            clusterUuid = cluster.uuid
            primaryStorageUuid = nfs.uuid
        }

        retryInSecs{
            GetCandidatePrimaryStoragesForCreatingVmAction getAction = new GetCandidatePrimaryStoragesForCreatingVmAction(
                    l3NetworkUuids : [l3.uuid],
                    imageUuid: image.uuid,
                    dataDiskOfferingUuids: [diskOffering.uuid],
                    sessionId: adminSession()
            )
            GetCandidatePrimaryStoragesForCreatingVmResult getResult = getAction.call().value
            List<PrimaryStorageInventory> rootVolumePrimaryStorages = getResult.rootVolumePrimaryStorages
            List<PrimaryStorageInventory> dataVolumePrimaryStorages = getResult.dataVolumePrimaryStorages.get(diskOffering.uuid)
            assert rootVolumePrimaryStorages.size() == 2
            assert [rootVolumePrimaryStorages[0].uuid, rootVolumePrimaryStorages[1].uuid].containsAll([local.uuid, nfs.uuid])
            assert dataVolumePrimaryStorages.size() == 2
            assert [dataVolumePrimaryStorages[0].uuid, dataVolumePrimaryStorages[1].uuid].containsAll([local.uuid, nfs.uuid])
        }



        logger.info("Test 201: not assign ps")
        createVmInstance {
            name = "vm201"
            cpuNum = 1
            memorySize = gb(1)
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            diskAOs = [
                [
                    "boot" : true,
                ],
                [
                    "size" : gb(1),
                ],
            ]
        }

        logger.info("Test 202: assign root volume local ps")
        def vm = createVmInstance {
            name = "vm202"
            cpuNum = 1
            memorySize = gb(1)
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            diskAOs = [
                [
                    "boot" : true,
                    "primaryStorageUuid" : local.uuid,
                ],
                [
                    "size" : gb(1),
                ],
            ]
        } as VmInstanceInventory
        checkVmRootDiskPs(vm, local.uuid)

        logger.info("Test 203: assign root volume nfs ps")
        vm = createVmInstance {
            name = "vm203"
            cpuNum = 1
            memorySize = gb(1)
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            diskAOs = [
                [
                    "boot" : true,
                    "primaryStorageUuid" : nfs.uuid,
                ],
                [
                    "size" : gb(1),
                ],
            ]
        } as VmInstanceInventory
        checkVmRootDiskPs(vm, nfs.uuid)

        logger.info("Test 204: assign data volume local ps")
        vm = createVmInstance {
            name = "vm204"
            cpuNum = 1
            memorySize = gb(1)
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            diskAOs = [
                [
                    "boot" : true,
                ],
                [
                    "size" : gb(1),
                    "primaryStorageUuid" : local.uuid,
                ],
            ]
        } as VmInstanceInventory
        checkVmRootDiskPs(vm, local.uuid)
        checkVmDataDiskPs(vm, local.uuid)

        logger.info("Test 205: assign data volume nfs ps")
        vm = createVmInstance {
            name = "vm205"
            cpuNum = 1
            memorySize = gb(1)
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            diskAOs = [
                [
                    "boot" : true,
                ],
                [
                    "size" : gb(1),
                    "primaryStorageUuid" : nfs.uuid,
                ],
            ]
        } as VmInstanceInventory
        checkVmRootDiskPs(vm, local.uuid)
        checkVmDataDiskPs(vm, nfs.uuid)

        logger.info("Test 206: assign root volume local ps, data volume local ps")
        vm = createVmInstance {
            name = "vm206"
            cpuNum = 1
            memorySize = gb(1)
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            diskAOs = [
                [
                    "boot" : true,
                    "primaryStorageUuid" : local.uuid,
                ],
                [
                    "size" : gb(1),
                    "primaryStorageUuid" : local.uuid,
                ],
            ]
        } as VmInstanceInventory
        checkVmRootDiskPs(vm, local.uuid)
        checkVmDataDiskPs(vm, local.uuid)

        logger.info("Test 207: assign root volume nfs ps, data volume local ps")
        vm = createVmInstance {
            name = "vm207"
            cpuNum = 1
            memorySize = gb(1)
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            diskAOs = [
                [
                    "boot" : true,
                    "primaryStorageUuid" : nfs.uuid,
                ],
                [
                    "size" : gb(1),
                    "primaryStorageUuid" : local.uuid,
                ],
            ]
        } as VmInstanceInventory
        checkVmRootDiskPs(vm, nfs.uuid)
        checkVmDataDiskPs(vm, local.uuid)

        logger.info("Test 208: assign root volume nfs ps, data volume nfs ps")
        vm = createVmInstance {
            name = "vm208"
            cpuNum = 1
            memorySize = gb(1)
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            diskAOs = [
                [
                    "boot" : true,
                    "primaryStorageUuid" : nfs.uuid,
                ],
                [
                    "size" : gb(1),
                    "primaryStorageUuid" : nfs.uuid,
                ],
            ]
        } as VmInstanceInventory
        checkVmRootDiskPs(vm, nfs.uuid)
        checkVmDataDiskPs(vm, nfs.uuid)

        logger.info("Test 209: assign root volume local ps, data volume nfs ps")
        vm = createVmInstance {
            name = "vm209"
            cpuNum = 1
            memorySize = gb(1)
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            diskAOs = [
                [
                    "boot" : true,
                    "primaryStorageUuid" : local.uuid,
                ],
                [
                    "size" : gb(1),
                    "primaryStorageUuid" : nfs.uuid,
                ],
            ]
        } as VmInstanceInventory
        checkVmRootDiskPs(vm, local.uuid)
        checkVmDataDiskPs(vm, nfs.uuid)
    }

    void NfsAndNfs(){
        ClusterInventory cluster = env.inventoryByName("cluster") as ClusterInventory
        PrimaryStorageInventory nfs = env.inventoryByName("nfs") as PrimaryStorageInventory
        PrimaryStorageInventory nfs2 = env.inventoryByName("nfs2") as PrimaryStorageInventory
        PrimaryStorageInventory local = env.inventoryByName("local") as PrimaryStorageInventory
        DiskOfferingInventory diskOffering = env.inventoryByName("diskOffering") as DiskOfferingInventory
        ImageInventory image = env.inventoryByName("image") as ImageInventory
        L3NetworkInventory l3 = env.inventoryByName("l3") as L3NetworkInventory

        detachPrimaryStorageFromCluster {
            clusterUuid = cluster.uuid
            primaryStorageUuid = local.uuid
        }

        attachPrimaryStorageToCluster {
            clusterUuid = cluster.uuid
            primaryStorageUuid = nfs2.uuid
        }

        retryInSecs{
            GetCandidatePrimaryStoragesForCreatingVmAction getAction = new GetCandidatePrimaryStoragesForCreatingVmAction(
                    l3NetworkUuids : [l3.uuid],
                    imageUuid: image.uuid,
                    dataDiskOfferingUuids: [diskOffering.uuid],
                    sessionId: adminSession()
            )
            GetCandidatePrimaryStoragesForCreatingVmResult getResult = getAction.call().value
            List<PrimaryStorageInventory> rootVolumePrimaryStorages = getResult.rootVolumePrimaryStorages
            List<PrimaryStorageInventory> dataVolumePrimaryStorages = getResult.dataVolumePrimaryStorages.get(diskOffering.uuid)
            assert rootVolumePrimaryStorages.size() == 2
            assert [rootVolumePrimaryStorages[0].uuid, rootVolumePrimaryStorages[1].uuid].containsAll([nfs.uuid, nfs2.uuid])
            assert dataVolumePrimaryStorages.size() == 2
            assert [dataVolumePrimaryStorages[0].uuid, dataVolumePrimaryStorages[1].uuid].containsAll([nfs.uuid, nfs2.uuid])
        }

        logger.info("Test 301: not assign ps")
        createVmInstance {
            name = "vm"
            cpuNum = 1
            memorySize = gb(1)
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            diskAOs = [
                [
                    "boot" : true,
                ],
                [
                    "size" : gb(1),
                ],
            ]
        }

        logger.info("Test 302: assign root volume nfs ps")
        def vm = createVmInstance {
            name = "vm1"
            cpuNum = 1
            memorySize = gb(1)
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            diskAOs = [
                [
                    "boot" : true,
                    "primaryStorageUuid" : nfs.uuid,
                ],
                [
                    "size" : gb(1),
                ],
            ]
        } as VmInstanceInventory
        checkVmRootDiskPs(vm, nfs.uuid)

        logger.info("Test 303: assign root volume ps")
        vm = createVmInstance {
            name = "vm2"
            cpuNum = 1
            memorySize = gb(1)
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            diskAOs = [
                [
                    "boot" : true,
                    "primaryStorageUuid" : nfs2.uuid,
                ],
                [
                    "size" : gb(1),
                ],
            ]
        } as VmInstanceInventory
        checkVmRootDiskPs(vm, nfs2.uuid)

        logger.info("Test 304: assign data volume ps")
        vm = createVmInstance {
            name = "vm3"
            cpuNum = 1
            memorySize = gb(1)
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            diskAOs = [
                [
                    "boot" : true,
                ],
                [
                    "size" : gb(1),
                    "primaryStorageUuid" : nfs.uuid,
                ],
            ]
        } as VmInstanceInventory
        checkVmDataDiskPs(vm, nfs.uuid)

        logger.info("Test 305: assign data, root volume ps")
        vm = createVmInstance {
            name = "vm4"
            cpuNum = 1
            memorySize = gb(1)
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            diskAOs = [
                [
                    "boot" : true,
                    "primaryStorageUuid" : nfs.uuid,
                ],
                [
                    "size" : gb(1),
                    "primaryStorageUuid" : nfs2.uuid,
                ],
            ]
        } as VmInstanceInventory
        checkVmRootDiskPs(vm, nfs.uuid)
        checkVmDataDiskPs(vm, nfs2.uuid)

        detachPrimaryStorageFromCluster {
            clusterUuid = cluster.uuid
            primaryStorageUuid = nfs.uuid
        }
        detachPrimaryStorageFromCluster {
            clusterUuid = cluster.uuid
            primaryStorageUuid = nfs2.uuid
        }
    }

    static void checkVmRootDiskPs(VmInstanceInventory vm, String psUuid){
        assert vm.allVolumes.size() > 0
        for (def disk : vm.allVolumes as List<VolumeInventory>){
            if(disk.uuid == vm.rootVolumeUuid){
                assert psUuid == disk.primaryStorageUuid
                return
            }
        }
    }

    static void checkVmDataDiskPs(VmInstanceInventory vm, String psUuid){
        assert vm.allVolumes.size() > 1
        for (def disk : vm.allVolumes as List<VolumeInventory>){
            if(disk.uuid != vm.rootVolumeUuid){
                assert psUuid == disk.primaryStorageUuid
            }
        }
    }

}
