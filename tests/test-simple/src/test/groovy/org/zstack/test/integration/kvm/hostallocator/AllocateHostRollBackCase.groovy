package org.zstack.test.integration.kvm.hostallocator

import org.zstack.sdk.HostInventory
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.allocator.HostAllocateExtension
import org.zstack.sdk.GetCpuMemoryCapacityResult

class AllocateHostRollBackCase extends SubCase {
    EnvSpec env

    HostInventory host

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
        spring {
            include("HostAllocateExtension.xml")
        }
    }

    @Override
    void environment() {
        env = makeEnv {
            instanceOffering {
                name = "instanceOffering"
                memory = gb(4)
                cpu = 2
            }

            zone{
                name = "zone"

                cephPrimaryStorage {
                    name="ceph-pri"
                    description="Test"
                    totalCapacity = gb(100)
                    availableCapacity= gb(100)
                    url="ceph://pri"
                    fsid="7ff218d9-f525-435f-8a40-3618d1772a64"
                    monUrls=["root:password@localhost/?monPort=7777"]
                }

                cluster {
                    name = "local-cluster"
                    hypervisorType = "KVM"

                    kvm {
                        name = "host1"
                        managementIp = "127.0.0.1"
                        username = "root"
                        password = "password"
                    }

                    kvm {
                        name = "host2"
                        managementIp = "127.0.0.2"
                        username = "root"
                        password = "password"
                    }

                    kvm {
                        name = "host3"
                        managementIp = "127.0.0.3"
                        username = "root"
                        password = "password"
                    }

                    kvm {
                        name = "host4"
                        managementIp = "127.0.0.4"
                        username = "root"
                        password = "password"
                    }

                    kvm {
                        name = "host5"
                        managementIp = "127.0.0.5"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("local-pri")
                    attachL2Network("l2")
                }

                cluster {
                    name = "ceph-cluster"
                    hypervisorType = "KVM"

                    kvm {
                        name = "host6"
                        managementIp = "localhost"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("ceph-pri")
                    attachL2Network("l2")
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

                localPrimaryStorage {
                    name = "local-pri"
                    url = "/local_ps"
                }

                cephBackupStorage {
                    name="ceph-bk"
                    description="Test"
                    url = "/bk"
                    fsid ="7ff218d9-f525-435f-8a40-3618d1772a64"
                    monUrls = ["root:password@localhost/?monPort=7777"]
                    totalCapacity = gb(100)
                    availableCapacity= gb(100)

                    image {
                        name = "image"
                        url  = "http://zstack.org/download/image.qcow2"
                    }
                }

                attachBackupStorage("ceph-bk")
            }
        }
    }

    @Override
    void test() {
        env.create {
            testAllocateMetFailAtExtensionPointAndSuccessfullyRollback()
        }
    }

    void testAllocateMetFailAtExtensionPointAndSuccessfullyRollback() {
        host = env.inventoryByName("host6") as HostInventory
        def image = env.inventoryByName("image") as ImageInventory
        def l3 = env.inventoryByName("l3") as L3NetworkInventory

        HostAllocateExtension ext = bean(HostAllocateExtension.class)
        ext.setErrorOut(true)

        GetCpuMemoryCapacityResult beforeChangedCapacity = getCpuMemoryCapacity {
            hostUuids = [host.uuid]
        } as GetCpuMemoryCapacityResult

        expect(AssertionError.class) {
            createVmInstance {
                name = "vm1"
                imageUuid = image.uuid
                l3NetworkUuids = [l3.uuid]
                cpuNum = 2
                memorySize = gb(4)
                hostUuid = host.uuid
            } as VmInstanceInventory
        }

        retryInSecs {
            GetCpuMemoryCapacityResult afterChangedCapacity = getCpuMemoryCapacity {
                hostUuids = [host.uuid]
            } as GetCpuMemoryCapacityResult

            assert afterChangedCapacity.availableCpu == beforeChangedCapacity.availableCpu
            assert afterChangedCapacity.availableMemory == beforeChangedCapacity.availableMemory
        }

        ext.setErrorOut(false)
    }
}

