package org.zstack.test.integration.storage.primary.local_nfs

import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.header.image.ImageConstant
import org.zstack.header.storage.primary.PrimaryStorageCapacityVO
import org.zstack.header.storage.primary.PrimaryStorageCapacityVO_
import org.zstack.header.storage.primary.PrimaryStorageStateEvent
import org.zstack.header.volume.VolumeStatus
import org.zstack.header.volume.VolumeVO
import org.zstack.header.volume.VolumeVO_
import org.zstack.sdk.ClusterInventory
import org.zstack.sdk.DiskOfferingInventory
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VolumeInventory
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

/**
 * Created by AlanJager on 2017/7/6.
 */

class LocalNfsMultiCombineCase extends SubCase {
    EnvSpec env
    PrimaryStorageInventory local
    PrimaryStorageInventory local2
    PrimaryStorageInventory nfs
    PrimaryStorageInventory nfs2
    ImageInventory qcow2
    ImageInventory iso
    L3NetworkInventory l3
    ClusterInventory cluster

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
                    name = "iso"
                    mediaType = ImageConstant.ImageMediaType.ISO
                    format = ImageConstant.ISO_FORMAT_STRING
                    url = "http://zstack.org/download/test.iso"
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
                    attachPrimaryStorage("nfs")
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
            local = env.inventoryByName("local") as PrimaryStorageInventory
            local2 = env.inventoryByName("local2") as PrimaryStorageInventory
            nfs = env.inventoryByName("nfs") as PrimaryStorageInventory
            nfs2 = env.inventoryByName("nfs2") as PrimaryStorageInventory
            qcow2 = env.inventoryByName("image") as ImageInventory
            iso = env.inventoryByName("iso") as ImageInventory
            l3 = env.inventoryByName("l3") as L3NetworkInventory
            cluster = env.inventoryByName("cluster") as ClusterInventory
            test2Local1NfsQcow2()
            test1Local2NfsQcow2()
            test2Local2NfsQcow2()
            test2Local1NfsISO()
            test1Local2NfsISO()
            test2Local2NfsISO()
        }
    }

    void test2Local1NfsQcow2() {
        logger.info("Test 101: not assign ps")
        createVmInstance {
            name = "vm101"
            cpuNum = 1
            memorySize = gb(1)
            imageUuid = qcow2.uuid
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

        logger.info("Test 102: assign ps")
        createVmInstance {
            name = "vm102"
            cpuNum = 1
            memorySize = gb(1)
            imageUuid = qcow2.uuid
            l3NetworkUuids = [l3.uuid]
            primaryStorageUuidForRootVolume = local.uuid
            diskAOs = [
                [
                    "boot" : true,
                ],
                [
                    "size" : gb(1),
                    "primaryStorageUuid": local.uuid,
                ],
            ]
        }

        logger.info("Test 103:")
        createVmInstance {
            delegate.name = "vm103"
            delegate.cpuNum = 1
            delegate.memorySize = gb(1)
            delegate.imageUuid = qcow2.uuid
            delegate.l3NetworkUuids = [l3.uuid]
            delegate.diskAOs = [
                [
                    "boot" : true,
                    "primaryStorageUuid": nfs.uuid,
                ],
                [
                    "size" : gb(1),
                ],
            ]
        }

        logger.info("Test 104:")
        createVmInstance {
            name = "vm104"
            cpuNum = 1
            memorySize = gb(1)
            imageUuid = qcow2.uuid
            l3NetworkUuids = [l3.uuid]
            diskAOs = [
                [
                    "boot" : true,
                ],
                [
                    "size" : gb(1),
                    "primaryStorageUuid": nfs.uuid,
                ],
            ]
        }

        logger.info("Test 105:")
        createVmInstance {
            delegate.name = "vm105"
            delegate.cpuNum = 1
            delegate.memorySize = gb(1)
            delegate.imageUuid = qcow2.uuid
            delegate.l3NetworkUuids = [l3.uuid]
            delegate.diskAOs = [
                [
                    "boot" : true,
                ],
                [
                    "size" : gb(1),
                    "primaryStorageUuid": local.uuid,
                ],
            ]
        }

        logger.info("Test 106: assign data nfs , root volume local ps")
        def vm = createVmInstance {
            name = "vm106"
            cpuNum = 1
            memorySize = gb(1)
            imageUuid = qcow2.uuid
            l3NetworkUuids = [l3.uuid]
            diskAOs = [
                [
                    "boot" : true,
                    "primaryStorageUuid": local.uuid,
                ],
                [
                    "size" : gb(1),
                    "primaryStorageUuid": nfs.uuid,
                ],
            ]
        } as VmInstanceInventory
        checkVmDataDiskPs(vm, nfs.uuid)
        checkVmRootDiskPs(vm, local.uuid)

        logger.info("Test 107: assign data ls , root volume local ps")
        vm = createVmInstance {
            delegate.name = "vm107"
            delegate.cpuNum = 1
            delegate.memorySize = gb(1)
            delegate.imageUuid = qcow2.uuid
            delegate.l3NetworkUuids = [l3.uuid]
            delegate.diskAOs = [
                [
                    "boot" : true,
                    "primaryStorageUuid": local.uuid,
                ],
                [
                    "size" : gb(1),
                    "primaryStorageUuid": local.uuid,
                ],
            ]
        } as VmInstanceInventory
        checkVmRootDiskPs(vm, local.uuid)
        checkVmDataDiskPs(vm, local.uuid)

        logger.info("Test 108: assign data nfs , root volume nfs ps")
        vm = createVmInstance {
            delegate.name = "vm108"
            delegate.cpuNum = 1
            delegate.memorySize = gb(1)
            delegate.imageUuid = qcow2.uuid
            delegate.l3NetworkUuids = [l3.uuid]
            delegate.diskAOs = [
                [
                    "boot" : true,
                    "primaryStorageUuid": nfs.uuid,
                ],
                [
                    "size" : gb(1),
                    "primaryStorageUuid": nfs.uuid,
                ],
            ]
        } as VmInstanceInventory
        checkVmRootDiskPs(vm, nfs.uuid)
        checkVmDataDiskPs(vm, nfs.uuid)

        logger.info("Test 109: assign data local, root volume nfs ps")
        vm = createVmInstance {
            delegate.name = "vm109"
            delegate.cpuNum = 1
            delegate.memorySize = gb(1)
            delegate.imageUuid = qcow2.uuid
            delegate.l3NetworkUuids = [l3.uuid]
            delegate.diskAOs = [
                [
                    "boot" : true,
                    "primaryStorageUuid": nfs.uuid,
                ],
                [
                    "size" : gb(1),
                    "primaryStorageUuid": local.uuid,
                ],
            ]
        } as VmInstanceInventory
        checkVmRootDiskPs(vm, nfs.uuid)
        checkVmDataDiskPs(vm, local.uuid)
    }

    void test1Local2NfsQcow2() {
        detachPrimaryStorageFromCluster {
            primaryStorageUuid = local2.uuid
            clusterUuid = cluster.uuid
        }

        attachPrimaryStorageToCluster {
            primaryStorageUuid = nfs2.uuid
            clusterUuid = cluster.uuid
        }

        long originCap = Q.New(PrimaryStorageCapacityVO.class).select(PrimaryStorageCapacityVO_.availableCapacity)
                .eq(PrimaryStorageCapacityVO_.uuid, nfs2.uuid)
                .findValue()

        SQL.New(PrimaryStorageCapacityVO.class)
                .eq(PrimaryStorageCapacityVO_.uuid, nfs2.uuid)
                .set(PrimaryStorageCapacityVO_.availableCapacity, 0L)
                .update()

        logger.info("Test 201: not assign ps")
        createVmInstance {
            name = "vm201"
            cpuNum = 1
            memorySize = gb(1)
            imageUuid = qcow2.uuid
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

        assert !Q.New(VolumeVO.class).eq(VolumeVO_.status, VolumeStatus.NotInstantiated).isExists()
        SQL.New(PrimaryStorageCapacityVO.class)
                .eq(PrimaryStorageCapacityVO_.uuid, nfs2.uuid)
                .set(PrimaryStorageCapacityVO_.availableCapacity, originCap)
                .update()

        logger.info("Test 202: assign root volume local ps")
        createVmInstance {
            name = "vm202"
            cpuNum = 1
            memorySize = gb(1)
            imageUuid = qcow2.uuid
            l3NetworkUuids = [l3.uuid]
            diskAOs = [
                [
                    "boot" : true,
                    "primaryStorageUuid": local.uuid,
                ],
                [
                    "size" : gb(1),
                    "primaryStorageUuid": local.uuid,
                ],
            ]
        }

        logger.info("Test 203: assign root volume nfs ps")
        createVmInstance {
            name = "vm203"
            cpuNum = 1
            memorySize = gb(1)
            imageUuid = qcow2.uuid
            l3NetworkUuids = [l3.uuid]
            diskAOs = [
                [
                    "boot" : true,
                    "primaryStorageUuid": nfs.uuid,
                ],
                [
                    "size" : gb(1),
                ],
            ]
        }

        logger.info("Test 204: assign data volume nfs ps")
        createVmInstance {
            name = "vm204"
            cpuNum = 1
            memorySize = gb(1)
            imageUuid = qcow2.uuid
            l3NetworkUuids = [l3.uuid]
            diskAOs = [
                [
                    "boot" : true,
                ],
                [
                    "size" : gb(1),
                    "primaryStorageUuid": nfs.uuid,
                ],
            ]
        }

        logger.info("Test 205: assign data volume local ps")
        createVmInstance {
            name = "vm205"
            cpuNum = 1
            memorySize = gb(1)
            imageUuid = qcow2.uuid
            l3NetworkUuids = [l3.uuid]
            diskAOs = [
                [
                    "boot" : true,
                ],
                [
                    "size" : gb(1),
                    "primaryStorageUuid": local.uuid,
                ],
            ]
        }

        logger.info("Test 206: assign data nfs, root volume local ps")
        def vm = createVmInstance {
            name = "vm206"
            cpuNum = 1
            memorySize = gb(1)
            imageUuid = qcow2.uuid
            l3NetworkUuids = [l3.uuid]
            diskAOs = [
                [
                    "boot" : true,
                    "primaryStorageUuid": local.uuid,
                ],
                [
                    "size" : gb(1),
                    "primaryStorageUuid": nfs.uuid,
                ],
            ]
        } as VmInstanceInventory
        checkVmDataDiskPs(vm, nfs.uuid)
        checkVmRootDiskPs(vm, local.uuid)

        logger.info("Test 207: assign data local, root volume local ps")
        vm = createVmInstance {
            name = "vm207"
            cpuNum = 1
            memorySize = gb(1)
            imageUuid = qcow2.uuid
            l3NetworkUuids = [l3.uuid]
            diskAOs = [
                [
                    "boot" : true,
                    "primaryStorageUuid": local.uuid,
                ],
                [
                    "size" : gb(1),
                    "primaryStorageUuid": local.uuid,
                ],
            ]
        } as VmInstanceInventory
        checkVmRootDiskPs(vm, local.uuid)
        checkVmDataDiskPs(vm, local.uuid)

        logger.info("Test 208: assign data nfs , root volume nfs ps")
        vm = createVmInstance {
            name = "vm208"
            cpuNum = 1
            memorySize = gb(1)
            imageUuid = qcow2.uuid
            l3NetworkUuids = [l3.uuid]
            diskAOs = [
                [
                    "boot" : true,
                    "primaryStorageUuid": nfs.uuid,
                ],
                [
                    "size" : gb(1),
                    "primaryStorageUuid": nfs.uuid,
                ],
            ]
        } as VmInstanceInventory
        checkVmRootDiskPs(vm, nfs.uuid)
        checkVmDataDiskPs(vm, nfs.uuid)

        logger.info("Test 209: assign data local , root volume nfs ps")
        vm = createVmInstance {
            name = "vm209"
            cpuNum = 1
            memorySize = gb(1)
            imageUuid = qcow2.uuid
            l3NetworkUuids = [l3.uuid]
            diskAOs = [
                [
                    "boot" : true,
                    "primaryStorageUuid": nfs.uuid,
                ],
                [
                    "size" : gb(1),
                    "primaryStorageUuid": local.uuid,
                ],
            ]
        } as VmInstanceInventory
        checkVmRootDiskPs(vm, nfs.uuid)
        checkVmDataDiskPs(vm, local.uuid)
    }

    void test2Local2NfsQcow2() {
        attachPrimaryStorageToCluster {
            primaryStorageUuid = local2.uuid
            clusterUuid = cluster.uuid
        }

        changePrimaryStorageState {
            uuid = local.uuid
            stateEvent = PrimaryStorageStateEvent.disable.toString()
        }

        logger.info("Test 301: not assign ps")
        createVmInstance {
            name = "vm301"
            cpuNum = 1
            memorySize = gb(1)
            imageUuid = qcow2.uuid
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

        detachPrimaryStorageFromCluster {
            primaryStorageUuid = local2.uuid
            clusterUuid = cluster.uuid
        }

        detachPrimaryStorageFromCluster {
            primaryStorageUuid = nfs.uuid
            clusterUuid = cluster.uuid
        }

        detachPrimaryStorageFromCluster {
            primaryStorageUuid = nfs2.uuid
            clusterUuid = cluster.uuid
        }

        changePrimaryStorageState {
            uuid = local.uuid
            stateEvent = PrimaryStorageStateEvent.enable.toString()
        }
    }

    void test2Local1NfsISO() {
        attachPrimaryStorageToCluster {
            primaryStorageUuid = local2.uuid
            clusterUuid = cluster.uuid
        }

        attachPrimaryStorageToCluster {
            primaryStorageUuid = nfs.uuid
            clusterUuid = cluster.uuid
        }

        logger.info("Test 401: not assign ps")
        createVmInstance {
            name = "vm401"
            cpuNum = 1
            memorySize = gb(1)
            imageUuid = iso.uuid
            l3NetworkUuids = [l3.uuid]
            diskAOs = [
                [
                    "boot" : true,
                    "size": gb(1),
                ],
                [
                    "size" : gb(1),
                ],
            ]
        }

        logger.info("Test 402: assign root volume local ps")
        createVmInstance {
            name = "vm402"
            cpuNum = 1
            memorySize = gb(1)
            imageUuid = iso.uuid
            l3NetworkUuids = [l3.uuid]
            diskAOs = [
                [
                    "boot" : true,
                    "size": gb(1),
                    "primaryStorageUuid": local.uuid,
                ],
                [
                    "size" : gb(1),
                ],
            ]
        }

        logger.info("Test 403: assign root volume nfs ps")
        createVmInstance {
            name = "vm403"
            cpuNum = 1
            memorySize = gb(1)
            imageUuid = iso.uuid
            l3NetworkUuids = [l3.uuid]
            diskAOs = [
                [
                    "boot" : true,
                    "size": gb(1),
                    "primaryStorageUuid": nfs.uuid,
                ],
                [
                    "size" : gb(1),
                ],
            ]
        }

        logger.info("Test 404: assign data volume nfs ps")
        createVmInstance {
            name = "vm404"
            cpuNum = 1
            memorySize = gb(1)
            imageUuid = iso.uuid
            l3NetworkUuids = [l3.uuid]
            diskAOs = [
                [
                    "boot" : true,
                    "size": gb(1),
                ],
                [
                    "size" : gb(1),
                    "primaryStorageUuid": nfs.uuid,
                ],
            ]
        }

        logger.info("Test 405: assign data volume local ps")
        createVmInstance {
            name = "vm405"
            cpuNum = 1
            memorySize = gb(1)
            imageUuid = iso.uuid
            l3NetworkUuids = [l3.uuid]
            diskAOs = [
                [
                    "boot" : true,
                    "size": gb(1),
                ],
                [
                    "size" : gb(1),
                    "primaryStorageUuid": local.uuid,
                ],
            ]
        }

        logger.info("Test 406: assign data nfs, root volume local ps")
        def vm = createVmInstance {
            name = "vm406"
            cpuNum = 1
            memorySize = gb(1)
            imageUuid = iso.uuid
            l3NetworkUuids = [l3.uuid]
            diskAOs = [
                [
                    "boot" : true,
                    "size": gb(1),
                    "primaryStorageUuid": local.uuid,
                ],
                [
                    "size" : gb(1),
                    "primaryStorageUuid": nfs.uuid,
                ],
            ]
        } as VmInstanceInventory
        checkVmDataDiskPs(vm, nfs.uuid)
        checkVmRootDiskPs(vm, local.uuid)

        logger.info("Test 407: assign data local, root volume local ps")
        vm = createVmInstance {
            name = "vm407"
            cpuNum = 1
            memorySize = gb(1)
            imageUuid = iso.uuid
            l3NetworkUuids = [l3.uuid]
            diskAOs = [
                [
                    "boot" : true,
                    "size": gb(1),
                    "primaryStorageUuid": local.uuid,
                ],
                [
                    "size" : gb(1),
                    "primaryStorageUuid": local.uuid,
                ],
            ]
        } as VmInstanceInventory
        checkVmDataDiskPs(vm, local.uuid)
        checkVmRootDiskPs(vm, local.uuid)

        logger.info("Test 408: assign data nfs, root volume nfs ps")
        vm = createVmInstance {
            name = "vm408"
            cpuNum = 1
            memorySize = gb(1)
            imageUuid = iso.uuid
            l3NetworkUuids = [l3.uuid]
            diskAOs = [
                [
                    "boot" : true,
                    "size": gb(1),
                    "primaryStorageUuid": nfs.uuid,
                ],
                [
                    "size" : gb(1),
                    "primaryStorageUuid": nfs.uuid,
                ],
            ]
        } as VmInstanceInventory
        checkVmDataDiskPs(vm, nfs.uuid)
        checkVmRootDiskPs(vm, nfs.uuid)

        logger.info("Test 409: assign data local, root volume nfs ps")
        vm = createVmInstance {
            name = "vm409"
            cpuNum = 1
            memorySize = gb(1)
            imageUuid = iso.uuid
            l3NetworkUuids = [l3.uuid]
            diskAOs = [
                [
                    "boot" : true,
                    "size": gb(1),
                    "primaryStorageUuid": nfs.uuid,
                ],
                [
                    "size" : gb(1),
                    "primaryStorageUuid": local.uuid,
                ],
            ]
        } as VmInstanceInventory
        checkVmRootDiskPs(vm, nfs.uuid)
        checkVmDataDiskPs(vm, local.uuid)
    }

    void test1Local2NfsISO() {
        detachPrimaryStorageFromCluster {
            primaryStorageUuid = local2.uuid
            clusterUuid = cluster.uuid
        }

        attachPrimaryStorageToCluster {
            primaryStorageUuid = nfs2.uuid
            clusterUuid = cluster.uuid
        }

        logger.info("Test 501: not assign ps")
        createVmInstance {
            name = "vm501"
            cpuNum = 1
            memorySize = gb(1)
            imageUuid = iso.uuid
            l3NetworkUuids = [l3.uuid]
            diskAOs = [
                [
                    "boot" : true,
                    "size": gb(1),
                ],
                [
                    "size" : gb(1),
                ],
            ]
        }

        logger.info("Test 502: assign root volume local ps")
        createVmInstance {
            name = "vm502"
            cpuNum = 1
            memorySize = gb(1)
            imageUuid = iso.uuid
            l3NetworkUuids = [l3.uuid]
            diskAOs = [
                [
                    "boot" : true,
                    "size": gb(1),
                    "primaryStorageUuid": local.uuid,
                ],
                [
                    "size" : gb(1),
                ],
            ]
        }

        logger.info("Test 503: assign root volume nfs ps")
        createVmInstance {
            name = "vm503"
            cpuNum = 1
            memorySize = gb(1)
            imageUuid = iso.uuid
            l3NetworkUuids = [l3.uuid]
            diskAOs = [
                [
                    "boot" : true,
                    "size": gb(1),
                    "primaryStorageUuid": nfs.uuid,
                ],
                [
                    "size" : gb(1),
                ],
            ]
        }

        logger.info("Test 504: assign data volume nfs ps")
        createVmInstance {
            name = "vm504"
            cpuNum = 1
            memorySize = gb(1)
            imageUuid = iso.uuid
            l3NetworkUuids = [l3.uuid]
            diskAOs = [
                [
                    "boot" : true,
                    "size": gb(1),
                ],
                [
                    "size" : gb(1),
                    "primaryStorageUuid": nfs.uuid,
                ],
            ]
        }

        logger.info("Test 505: assign data volume local ps")
        createVmInstance {
            name = "vm505"
            cpuNum = 1
            memorySize = gb(1)
            imageUuid = iso.uuid
            l3NetworkUuids = [l3.uuid]
            diskAOs = [
                [
                    "boot" : true,
                    "size": gb(1),
                ],
                [
                    "size" : gb(1),
                    "primaryStorageUuid": local.uuid,
                ],
            ]
        }

        logger.info("Test 506: assign data nfs, root volume local ps")
        def vm = createVmInstance {
            name = "vm506"
            cpuNum = 1
            memorySize = gb(1)
            imageUuid = iso.uuid
            l3NetworkUuids = [l3.uuid]
            diskAOs = [
                [
                    "boot" : true,
                    "size": gb(1),
                    "primaryStorageUuid": local.uuid,
                ],
                [
                    "size" : gb(1),
                    "primaryStorageUuid": nfs.uuid,
                ],
            ]
        } as VmInstanceInventory
        checkVmDataDiskPs(vm, nfs.uuid)
        checkVmRootDiskPs(vm, local.uuid)

        logger.info("Test 507: assign data ls, root volume local ps")
        vm = createVmInstance {
            name = "vm507"
            cpuNum = 1
            memorySize = gb(1)
            imageUuid = iso.uuid
            l3NetworkUuids = [l3.uuid]
            diskAOs = [
                [
                    "boot" : true,
                    "size": gb(1),
                    "primaryStorageUuid": local.uuid,
                ],
                [
                    "size" : gb(1),
                    "primaryStorageUuid": local.uuid,
                ],
            ]
        } as VmInstanceInventory
        checkVmDataDiskPs(vm, local.uuid)
        checkVmRootDiskPs(vm, local.uuid)

        logger.info("Test 508: assign data nfs, root volume nfs ps")
        vm = createVmInstance {
            name = "vm508"
            cpuNum = 1
            memorySize = gb(1)
            imageUuid = iso.uuid
            l3NetworkUuids = [l3.uuid]
            diskAOs = [
                [
                    "boot" : true,
                    "size": gb(1),
                    "primaryStorageUuid": nfs.uuid,
                ],
                [
                    "size" : gb(1),
                    "primaryStorageUuid": nfs.uuid,
                ],
            ]
        } as VmInstanceInventory
        checkVmDataDiskPs(vm, nfs.uuid)
        checkVmRootDiskPs(vm, nfs.uuid)

        logger.info("Test 509: assign data local, root volume nfs ps")
        vm = createVmInstance {
            name = "vm509"
            cpuNum = 1
            memorySize = gb(1)
            imageUuid = iso.uuid
            l3NetworkUuids = [l3.uuid]
            diskAOs = [
                [
                    "boot" : true,
                    "size": gb(1),
                    "primaryStorageUuid": nfs.uuid,
                ],
                [
                    "size" : gb(1),
                    "primaryStorageUuid": local.uuid,
                ],
            ]
        } as VmInstanceInventory
        checkVmRootDiskPs(vm, nfs.uuid)
        checkVmDataDiskPs(vm, local.uuid)
    }

    void test2Local2NfsISO() {
        attachPrimaryStorageToCluster {
            primaryStorageUuid = local2.uuid
            clusterUuid = cluster.uuid
        }

        changePrimaryStorageState {
            uuid = local.uuid
            stateEvent = PrimaryStorageStateEvent.disable.toString()
        }

        logger.info("Test 601: not assign ps")
        createVmInstance {
            name = "vm601"
            cpuNum = 1
            memorySize = gb(1)
            imageUuid = iso.uuid
            l3NetworkUuids = [l3.uuid]
            diskAOs = [
                [
                    "boot" : true,
                    "size": gb(1),
                ],
                [
                    "size" : gb(1),
                ],
            ]
        }

        detachPrimaryStorageFromCluster {
            primaryStorageUuid = local2.uuid
            clusterUuid = cluster.uuid
        }

        detachPrimaryStorageFromCluster {
            primaryStorageUuid = nfs.uuid
            clusterUuid = cluster.uuid
        }

        detachPrimaryStorageFromCluster {
            primaryStorageUuid = nfs2.uuid
            clusterUuid = cluster.uuid
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
