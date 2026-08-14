package org.zstack.test.integration.kvm.vm

import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VolumeInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

/**
 * Created by lining on 2018/01/24.
 */
class DiskAOCase extends SubCase {
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

            vm {
                name = "vm1"
                memoryGB(8)
                cpu = 4
                disk {
                    sizeGB(20)
                    platform = "Linux"
                    guestOsType = "Linux"
                    architecture = "x86_64"
                }
                disk {
                    sizeGB(30)
                }
                useL3Networks("l3")
            }
        }
    }

    @Override
    void test() {
        env.create {
            testCheckVm1()
        }
    }
    
    void testCheckVm1() {
        logger.info("Test 001: test check VM 1")
        def vm = env.inventoryByName("vm1") as VmInstanceInventory
        assert vm.allVolumes.size() == 2

        def rootVolume = (vm.allVolumes as List<VolumeInventory>).find { it.type == "Root"}
        assert rootVolume != null
        assert rootVolume.size == SizeUnit.GIGABYTE.toByte(20)

        def dataVolume1 = (vm.allVolumes as List<VolumeInventory>).find { it.type == "Data"}
        assert dataVolume1 != null
        assert dataVolume1.size == SizeUnit.GIGABYTE.toByte(30)
    }

    @Override
    void clean() {
        env.delete()
    }
}
