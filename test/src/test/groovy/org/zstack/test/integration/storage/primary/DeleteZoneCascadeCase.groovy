package org.zstack.test.integration.storage.primary

import org.zstack.core.db.Q
import org.zstack.header.cluster.ClusterVO
import org.zstack.header.image.ImageConstant
import org.zstack.header.network.l3.L3NetworkVO
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.header.storage.primary.PrimaryStorageState
import org.zstack.header.storage.primary.PrimaryStorageStateEvent
import org.zstack.header.storage.primary.PrimaryStorageVO
import org.zstack.header.storage.primary.PrimaryStorageVO_
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.volume.VolumeVO
import org.zstack.header.zone.ZoneVO
import org.zstack.kvm.KVMHostVO
import org.zstack.network.securitygroup.SecurityGroupConstant
import org.zstack.network.service.virtualrouter.VirtualRouterConstant
import org.zstack.sdk.DiskOfferingInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VolumeInventory
import org.zstack.sdk.ZoneInventory
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

class DeleteZoneCascadeCase extends SubCase {
    EnvSpec env

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
                    url  = "http://zstack.org/download/vr.qcow2"
                }

                image {
                    name = "iso"
                    url  = "http://zstack.org/download/test.iso"
                    format = ImageConstant.ISO_FORMAT_STRING.toString()
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


                    kvm {
                        name = "kvm1"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("nfs")
                    attachL2Network("l2")
                }

                nfsPrimaryStorage {
                    name = "nfs"
                    url = "127.0.0.3:/nfs_root"
                }

                l2NoVlanNetwork {
                    name = "l2"
                    physicalInterface = "eth0"

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

                attachBackupStorage("sftp")
            }

            vm {
                name = "vm"
                useInstanceOffering("instanceOffering")
                useImage("image1")
                useL3Networks("pubL3")
                useRootDiskOffering("diskOffering")
            }

            vm {
                name = "vm2"
                useInstanceOffering("instanceOffering")
                useImage("image1")
                useL3Networks("pubL3")
                useRootDiskOffering("diskOffering")
            }

            vm {
                name = "vm3"
                useInstanceOffering("instanceOffering")
                useImage("image1")
                useL3Networks("pubL3")
                useRootDiskOffering("diskOffering")
            }
        }
    }

    @Override
    void test() {
        env.create {
            testDeleteZone()
        }
    }

    void testDeleteZone() {
        ZoneInventory zone = env.inventoryByName("zone")
        PrimaryStorageInventory ps = env.inventoryByName("nfs")
        DiskOfferingInventory diskOffering = env.inventoryByName("diskOffering")

        for (int i = 0; i < 5; i++) {
            createDataVolume {
                name = "dataVolume"
                diskOfferingUuid = diskOffering.uuid
                primaryStorageUuid = ps.uuid
            }
        }

        for (int i = 0; i < 5; i++) {
            VolumeInventory volume = createDataVolume {
                name = "dataVolume"
                diskOfferingUuid = diskOffering.uuid
                primaryStorageUuid = ps.uuid
            }
            deleteDataVolume {
                uuid = volume.uuid
            }
        }

        deleteZone {
            uuid = zone.uuid
        }

        assert !Q.New(ZoneVO.class).isExists()
        assert !Q.New(PrimaryStorageVO.class).isExists()
        assert !Q.New(ClusterVO.class).isExists()
        assert !Q.New(L3NetworkVO.class).isExists()
        assert !Q.New(KVMHostVO.class).isExists()
        assert !Q.New(VmInstanceVO.class).isExists()
        assert !Q.New(VolumeVO.class).isExists()
    }
}
