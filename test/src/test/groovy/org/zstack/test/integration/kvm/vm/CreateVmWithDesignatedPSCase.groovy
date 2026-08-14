package org.zstack.test.integration.kvm.vm

import org.zstack.core.Platform
import org.zstack.sdk.HostInventory
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase


class CreateVmWithDesignatedPSCase extends SubCase {
    EnvSpec env

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
                    name = "image"
                    url = "http://zstack.org/download/test.qcow2"
                }
            }

            zone {
                name = "zone"
                description = "test"

                cluster {
                    name = "cluster-1"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm1"
                        managementIp = "127.0.0.10"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("local1")
                    attachL2Network("l2")
                }

                cluster {
                    name = "cluster-2"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm2"
                        managementIp = "127.0.0.11"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("local2")
                    attachL2Network("l2")
                }

                localPrimaryStorage {
                    name = "local1"
                    url = "/local_ps"
                }

                localPrimaryStorage {
                    name = "local2"
                    url = "/local_ps"
                }

                l2NoVlanNetwork {
                    name = "l2"
                    physicalInterface = "eth0"

                    l3Network {
                        name = "l3"

                        ip {
                            startIp = "192.168.100.10"
                            endIp = "192.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.100.1"
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
            TestCreateVmWithDesignatedPS()
        }
    }

    void TestCreateVmWithDesignatedPS() {
        def ps_1 = env.inventoryByName("local1") as PrimaryStorageInventory
        def ps_2 = env.inventoryByName("local2") as PrimaryStorageInventory
        def image = env.inventoryByName("image") as ImageInventory
        def l3 = env.inventoryByName("l3") as L3NetworkInventory

        expectApiFailure({
            createVmInstance {
                name = "test_vm"
                cpuNum = 4
                memorySize = gb(8)
                l3NetworkUuids = [l3.uuid]
                imageUuid = image.uuid
                diskAOs = [
                    [
                        "boot" : true,
                        "primaryStorageUuid" : ps_1.uuid,
                    ],
                    [
                        "size" : gb(20),
                        "primaryStorageUuid" : ps_2.uuid,
                    ]
                ]
            }
        }) {
            // host1 -> ps_1
            // host2 -> ps_2
            assert delegate.code == "HOST_ALLOCATION.1001"
            assert delegate.opaque
            assert delegate.opaque["rejectedCandidates"] instanceof List
            def rejectedCandidates = (delegate.opaque["rejectedCandidates"] as List<Map<String, Object>>)
            assert rejectedCandidates.size() == 2

            def kvm1 = env.inventoryByName("kvm1") as HostInventory
            def candidates1 = rejectedCandidates.findAll {
                return it["hostUuid"] == kvm1.uuid
            }
            assert candidates1.size() == 1
            assert candidates1[0]["hostName"] == "kvm1"
            assert candidates1[0]["reject"] == "not accessible to the specific primary storage"
            assert candidates1[0]["rejectI18n"] == Platform.i18n("not accessible to the specific primary storage")
            assert candidates1[0]["rejectBy"] == "HostPrimaryStorageAllocatorFlow"

            def kvm2 = env.inventoryByName("kvm2") as HostInventory
            def candidates2 = rejectedCandidates.findAll {
                return it["hostUuid"] == kvm2.uuid
            }
            assert candidates2.size() == 1
            assert candidates2[0]["hostName"] == "kvm2"
            assert candidates2[0]["reject"] == "not accessible to the specific primary storage"
            assert candidates2[0]["rejectI18n"] == Platform.i18n("not accessible to the specific primary storage")
            assert candidates2[0]["rejectBy"] == "HostPrimaryStorageAllocatorFlow"
        }
    }

    @Override
    void clean() {
        env.delete()
    }
}
