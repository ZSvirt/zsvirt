package org.zstack.test.integration.kvm.vm


import org.zstack.sdk.ImageInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.TagInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

class CreateVmWithVolumeTagsCase extends SubCase {
    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
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
        }
    }

    @Override
    void test() {
        env.create {
            createVmWithVolumeTags()
        }
    }

    void createVmWithVolumeTags() {
        def image = env.inventoryByName("image1") as ImageInventory
        def pubL3 = env.inventoryByName("pubL3") as L3NetworkInventory

        def result = createVmInstance {
            delegate.name = "vm"
            delegate.cpuNum = 4
            delegate.memorySize = SizeUnit.GIGABYTE.toByte(8)
            delegate.l3NetworkUuids = [pubL3.uuid]
            delegate.imageUuid = image.uuid
            delegate.diskAOs = [
                [
                    boot : true,
                    systemTags : [
                        "volumeMaxIncrementalSnapshotNum::10"
                    ]
                ],
            ]
        } as VmInstanceInventory

        def tags = querySystemTag {
            conditions=["resourceUuid=${result.rootVolumeUuid}".toString()]
        } as List<TagInventory>
        assert tags.tag.contains("volumeMaxIncrementalSnapshotNum::10")
    }
}
