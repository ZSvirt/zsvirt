package org.zstack.test.integration.storage.primary

import org.zstack.core.db.Q
import org.zstack.header.cluster.ClusterVO
import org.zstack.header.image.ImageConstant
import org.zstack.header.network.l3.L3NetworkVO
import org.zstack.header.storage.primary.PrimaryStorageVO
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.volume.VolumeVO
import org.zstack.header.zone.ZoneVO
import org.zstack.kvm.KVMHostVO
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.sdk.VolumeInventory
import org.zstack.sdk.ZoneInventory
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

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
        env = makeEnv {
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
                cpu = 4
                memoryGB(8)
                useImage("image1")
                useL3Networks("pubL3")
                disk {
                    sizeGB(20)
                }
            }

            vm {
                name = "vm2"
                cpu = 4
                memoryGB(8)
                useImage("image1")
                useL3Networks("pubL3")
                disk {
                    sizeGB(20)
                }
            }

            vm {
                name = "vm3"
                cpu = 4
                memoryGB(8)
                useImage("image1")
                useL3Networks("pubL3")
                disk {
                    sizeGB(20)
                }
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

        for (int i = 0; i < 5; i++) {
            createDataVolume {
                name = "dataVolume"
                diskSize = gb(20)
                primaryStorageUuid = ps.uuid
            }
        }

        for (int i = 0; i < 5; i++) {
            VolumeInventory volume = createDataVolume {
                name = "dataVolume"
                diskSize = gb(20)
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
