package org.zstack.test.integration.kvm.hostallocator

import org.zstack.header.allocator.HostAllocatorConstant
import org.zstack.network.securitygroup.SecurityGroupConstant
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.*
import org.zstack.utils.data.SizeUnit

class LastHostPreferredAllocatorCase extends SubCase {
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
            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(2)
                cpu = 2
                allocatorStrategy = HostAllocatorConstant.LAST_HOST_PREFERRED_ALLOCATOR_STRATEGY_TYPE
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
            }

            zone {
                name = "zone"
                description = "test"

                cluster {
                    name = "cluster"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm1"
                        managementIp = "127.0.0.2"
                        username = "root"
                        password = "password"

                        totalCpu = 8
                        totalMem = SizeUnit.GIGABYTE.toByte(10)
                    }

                    kvm {
                        name = "kvm2"
                        managementIp = "127.0.0.3"
                        username = "root"
                        password = "password"

                        totalCpu = 8
                        totalMem = SizeUnit.GIGABYTE.toByte(10)
                    }

                    kvm {
                        name = "kvm3"
                        managementIp = "127.0.0.4"
                        username = "root"
                        password = "password"

                        totalCpu = 8
                        totalMem = SizeUnit.GIGABYTE.toByte(10)
                    }

                    attachPrimaryStorage("nfs")
                    attachL2Network("l2")
                }

                nfsPrimaryStorage {
                    name = "nfs"
                    url = "/nfs_root"
                }

                l2NoVlanNetwork {
                    name = "l2"
                    physicalInterface = "eth0"

                    l3Network {
                        name = "l3"

                        service {
                            provider = SecurityGroupConstant.SECURITY_GROUP_PROVIDER_TYPE
                            types = [SecurityGroupConstant.SECURITY_GROUP_NETWORK_SERVICE_TYPE]
                        }

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

            vm {
                name = "vm"
                useInstanceOffering("instanceOffering")
                useImage("image1")
                useL3Networks("l3")
                useHost("kvm1")
            }
        }
    }

    @Override
    void test() {
        env.create {
            testLastHostPreferredAllocator()
        }
    }

    void testLastHostPreferredAllocator() {
        VmInstanceInventory vm = env.inventoryByName("vm")
        assert vm.allocatorStrategy == HostAllocatorConstant.LAST_HOST_PREFERRED_ALLOCATOR_STRATEGY_TYPE

        String vmHostUuid = vm.getHostUuid()
        for (int i = 0; i < 30; i ++) {
            stopVmInstance {
                uuid = vm.uuid
            }

            vm = startVmInstance {
                uuid = vm.uuid
            }
            assert vm.hostUuid == vmHostUuid
        }

        for (int i = 0; i < 10; i ++) {
            vm = rebootVmInstance {
                uuid = vm.uuid
            }
            assert vm.hostUuid == vmHostUuid
        }

    }

}
