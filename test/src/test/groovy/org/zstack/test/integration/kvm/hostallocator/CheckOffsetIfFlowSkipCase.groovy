package org.zstack.test.integration.kvm.hostallocator

import org.zstack.compute.allocator.HostAllocatorGlobalConfig
import org.zstack.sdk.HostInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

class CheckOffsetIfFlowSkipCase extends SubCase{
    EnvSpec env
    VmInstanceInventory vm

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
            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(4)
                cpu = 1
            }

            diskOffering {
                name = "diskOffering"
                diskSize = SizeUnit.GIGABYTE.toByte(20)
            }

            zone {
                name = "zone"
                cluster {
                    name = "cluster"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm1"
                        managementIp = "127.0.0.1"
                        username = "root"
                        password = "password"
                    }

                    kvm {
                        name = "kvm2"
                        managementIp = "127.0.0.2"
                        username = "root"
                        password = "password"
                    }

                    kvm {
                        name = "kvm3"
                        managementIp = "127.0.0.3"
                        username = "root"
                        password = "password"
                    }

                    kvm {
                        name = "kvm4"
                        managementIp = "127.0.0.4"
                        username = "root"
                        password = "password"
                    }


                    attachPrimaryStorage("ps")
                    attachL2Network("l2")
                }


                localPrimaryStorage {
                    name = "ps"
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
            }


            vm {
                name = "vm"
                useInstanceOffering("instanceOffering")
                useImage("image1")
                useL3Networks("l3")
                useCluster("cluster")
            }
        }
    }

    @Override
    void test() {
        env.create {
            vm = env.inventoryByName("vm") as VmInstanceInventory
            testLocalHostFilterPagination()
            /**
             * 1. delete vm nic to skip AttachedL2NetworkAllocatorFlow
             * 2. set pagination and limit
             * 3. get candidate host
             */
            detachNicAndGetCandidateHost()
        }
    }

    void testLocalHostFilterPagination() {
        HostAllocatorGlobalConfig.USE_PAGINATION.updateValue(true)
        HostAllocatorGlobalConfig.PAGINATION_LIMIT.updateValue(1)
        stopVmInstance {
            uuid = vm.uuid
        }

        startVmInstance {
            uuid = vm.uuid
        }
    }

    void detachNicAndGetCandidateHost() {
        vm = detachL3NetworkFromVm {
            vmNicUuid = vm.vmNics.get(0).uuid
        } as VmInstanceInventory

        assert vm.vmNics.size() == 0

        HostAllocatorGlobalConfig.USE_PAGINATION.updateValue(true)
        HostAllocatorGlobalConfig.PAGINATION_LIMIT.updateValue(4)

        def hosts = getVmMigrationCandidateHosts {
            vmInstanceUuid = vm.uuid
        } as List<HostInventory>

        assert hosts.size() == 3


        HostAllocatorGlobalConfig.PAGINATION_LIMIT.updateValue(1)

        hosts = getVmMigrationCandidateHosts {
            vmInstanceUuid = vm.uuid
        } as List<HostInventory>

        assert hosts.size() == 1
    }
}
