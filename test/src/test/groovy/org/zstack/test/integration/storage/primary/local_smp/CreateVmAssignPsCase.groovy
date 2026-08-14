package org.zstack.test.integration.storage.primary.local_smp

import org.zstack.sdk.*
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

/**
 * Created by lining on 2017-09-30.
 */
class CreateVmAssignPsCase extends SubCase{
    EnvSpec env

    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
    }

    @Override
    void environment() {
        env = env{
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

                smpPrimaryStorage {
                    name = "smp"
                    url = "/smp"
                }

                smpPrimaryStorage {
                    name = "smp2"
                    url = "/smp2"
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
            localAndSmp()
            smpAndSmp()
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    void localAndSmp(){
        ClusterInventory cluster = env.inventoryByName("cluster") as ClusterInventory
        PrimaryStorageInventory smp = env.inventoryByName("smp") as PrimaryStorageInventory
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
            primaryStorageUuid = smp.uuid
        }

        retryInSecs {
            GetCandidatePrimaryStoragesForCreatingVmAction getAction = new GetCandidatePrimaryStoragesForCreatingVmAction(
                    l3NetworkUuids: [l3.uuid],
                    imageUuid: image.uuid,
                    dataDiskOfferingUuids: [diskOffering.uuid],
                    sessionId: adminSession()
            )
            GetCandidatePrimaryStoragesForCreatingVmResult getResult = getAction.call().value
            List<PrimaryStorageInventory> rootVolumePrimaryStorages = getResult.rootVolumePrimaryStorages
            List<PrimaryStorageInventory> dataVolumePrimaryStorages = getResult.dataVolumePrimaryStorages.get(diskOffering.uuid)
            assert rootVolumePrimaryStorages.size() == 2
            assert [rootVolumePrimaryStorages[0].uuid, rootVolumePrimaryStorages[1].uuid].containsAll([local.uuid, smp.uuid])
            assert dataVolumePrimaryStorages.size() == 2
            assert [dataVolumePrimaryStorages[0].uuid, dataVolumePrimaryStorages[1].uuid].containsAll([local.uuid, smp.uuid])
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

        logger.info("Test 102: assign root volume local ps")
        createVmInstance {
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
        }

        logger.info("Test 103: assign root volume smp ps")
        def vm = createVmInstance {
            name = "vm103"
            cpuNum = 1
            memorySize = gb(1)
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            diskAOs = [
                [
                    "boot" : true,
                    "primaryStorageUuid" : smp.uuid,
                ],
            ]
        } as VmInstanceInventory
        checkVmRootDiskPs(vm, smp.uuid)

        logger.info("Test 104: assign data volume local ps")
        createVmInstance {
            name = "vm104"
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
        }

        logger.info("Test 105: assign data volume smp ps")
        createVmInstance {
            name = "vm105"
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
                    "primaryStorageUuid" : smp.uuid,
                ],
            ]
        }

        logger.info("Test 106: assign root volume local ps, data volume local ps")
        vm = createVmInstance {
            name = "vm106"
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

        logger.info("Test 107: assign root volume smp ps, data volume local ps")
        vm = createVmInstance {
            name = "vm107"
            cpuNum = 1
            memorySize = gb(1)
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            diskAOs = [
                [
                    "boot" : true,
                    "primaryStorageUuid" : smp.uuid,
                ],
                [
                    "size" : gb(1),
                    "primaryStorageUuid" : local.uuid,
                ],
            ]
        } as VmInstanceInventory
        checkVmRootDiskPs(vm, smp.uuid)
        checkVmDataDiskPs(vm, local.uuid)

        logger.info("Test 108: assign root volume smp ps, data volume smp ps")
        vm = createVmInstance {
            name = "vm108"
            cpuNum = 1
            memorySize = gb(1)
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            diskAOs = [
                [
                    "boot" : true,
                    "primaryStorageUuid" : smp.uuid,
                ],
                [
                    "size" : gb(1),
                    "primaryStorageUuid" : smp.uuid,
                ],
            ]
        } as VmInstanceInventory
        checkVmRootDiskPs(vm, smp.uuid)
        checkVmDataDiskPs(vm, smp.uuid)

        logger.info("Test 109: assign root volume local ps, data volume smp ps")
        vm = createVmInstance {
            name = "vm109"
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
                    "primaryStorageUuid" : smp.uuid,
                ],
            ]
        } as VmInstanceInventory
        checkVmRootDiskPs(vm, local.uuid)
        checkVmDataDiskPs(vm, smp.uuid)
    }

    void smpAndSmp(){
        ClusterInventory cluster = env.inventoryByName("cluster") as ClusterInventory
        PrimaryStorageInventory smp = env.inventoryByName("smp") as PrimaryStorageInventory
        PrimaryStorageInventory smp2 = env.inventoryByName("smp2") as PrimaryStorageInventory
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
            primaryStorageUuid = smp2.uuid
        }

        retryInSecs {
            GetCandidatePrimaryStoragesForCreatingVmAction getAction = new GetCandidatePrimaryStoragesForCreatingVmAction(
                    l3NetworkUuids: [l3.uuid],
                    imageUuid: image.uuid,
                    dataDiskOfferingUuids: [diskOffering.uuid],
                    sessionId: adminSession()
            )
            GetCandidatePrimaryStoragesForCreatingVmResult getResult = getAction.call().value
            List<PrimaryStorageInventory> rootVolumePrimaryStorages = getResult.rootVolumePrimaryStorages
            List<PrimaryStorageInventory> dataVolumePrimaryStorages = getResult.dataVolumePrimaryStorages.get(diskOffering.uuid)
            assert rootVolumePrimaryStorages.size() == 2
            assert [rootVolumePrimaryStorages[0].uuid, rootVolumePrimaryStorages[1].uuid].containsAll([smp.uuid, smp2.uuid])
            assert dataVolumePrimaryStorages.size() == 2
            assert [dataVolumePrimaryStorages[0].uuid, dataVolumePrimaryStorages[1].uuid].containsAll([smp.uuid, smp2.uuid])
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

        logger.info("Test 202: assign root volume smp ps")
        def vm = createVmInstance {
            name = "vm202"
            cpuNum = 1
            memorySize = gb(1)
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            diskAOs = [
                [
                    "boot" : true,
                    "primaryStorageUuid" : smp.uuid,
                ],
                [
                    "size" : gb(1),
                ],
            ]
        } as VmInstanceInventory
        checkVmRootDiskPs(vm, smp.uuid)

        logger.info("Test 203: assign root volume ps")
        vm = createVmInstance {
            name = "vm203"
            cpuNum = 1
            memorySize = gb(1)
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            diskAOs = [
                [
                    "boot" : true,
                    "primaryStorageUuid" : smp2.uuid,
                ],
                [
                    "size" : gb(1),
                ],
            ]
        } as VmInstanceInventory
        checkVmRootDiskPs(vm, smp2.uuid)

        logger.info("Test 204: assign data volume ps")
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
                    "primaryStorageUuid" : smp.uuid,
                ],
            ]
        } as VmInstanceInventory
        checkVmDataDiskPs(vm, smp.uuid)

        logger.info("Test 205: assign data, root volume ps")
        vm = createVmInstance {
            name = "vm205"
            cpuNum = 1
            memorySize = gb(1)
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            diskAOs = [
                [
                    "boot" : true,
                    "primaryStorageUuid" : smp.uuid,
                ],
                [
                    "size" : gb(1),
                    "primaryStorageUuid" : smp2.uuid,
                ],
            ]
        } as VmInstanceInventory
        checkVmRootDiskPs(vm, smp.uuid)
        checkVmDataDiskPs(vm, smp2.uuid)

        detachPrimaryStorageFromCluster {
            clusterUuid = cluster.uuid
            primaryStorageUuid = smp.uuid
        }
        detachPrimaryStorageFromCluster {
            clusterUuid = cluster.uuid
            primaryStorageUuid = smp2.uuid
        }
    }

    static void checkVmRootDiskPs(VmInstanceInventory vm, String psUuid){
        assert vm.allVolumes.size() > 0
        for(VolumeInventory disk : vm.allVolumes as List<VolumeInventory>){
            if(disk.uuid == vm.rootVolumeUuid){
                assert psUuid == disk.primaryStorageUuid
                return
            }
        }
    }

    static void checkVmDataDiskPs(VmInstanceInventory vm, String psUuid){
        assert vm.allVolumes.size() > 1
        for(VolumeInventory disk : vm.allVolumes as List<VolumeInventory>){
            if(disk.uuid != vm.rootVolumeUuid){
                assert psUuid == disk.primaryStorageUuid
            }
        }
    }

}
