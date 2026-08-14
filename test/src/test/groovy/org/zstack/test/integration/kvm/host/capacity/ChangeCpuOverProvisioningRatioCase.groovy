package org.zstack.test.integration.kvm.host.capacity

import org.zstack.compute.host.HostGlobalConfig
import org.zstack.sdk.HostInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.*
import org.zstack.utils.data.SizeUnit

/**
 * Created by lining on 2017/7/06.
 */
class ChangeCpuOverProvisioningRatioCase extends SubCase {

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
                    name = "image1"
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
                        managementIp = "127.0.0.1"
                        username = "root"
                        password = "password"
                        totalCpu = 1
                        totalMem = SizeUnit.GIGABYTE.toByte(4)
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
                name = "vm"
                cpu = 1
                memorySize = SizeUnit.GIGABYTE.toByte(1)
                useImage("image1")
                useL3Networks("l3")
            }
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void test() {
        env.create {
            checkHostAvailableCpuCapacity()
        }
    }

    void checkHostAvailableCpuCapacity(){
        HostGlobalConfig.HOST_CPU_OVER_PROVISIONING_RATIO.updateValue(2)
        HostInventory host = env.inventoryByName("kvm")
        VmInstanceInventory vm = env.inventoryByName("vm")

        retryInSecs(2){
            host = queryHost {
                conditions=["uuid=${host.uuid}"]
            }[0]
            assert host.availableCpuCapacity == 1
        }

        VmInstanceInventory newVm = createVmInstance {
            name = "newVm"
            cpuNum = 1
            memorySize = SizeUnit.GIGABYTE.toByte(1)
            imageUuid = vm.imageUuid
            l3NetworkUuids = [vm.defaultL3NetworkUuid]
        }

        HostGlobalConfig.HOST_CPU_OVER_PROVISIONING_RATIO.updateValue(1)

        retryInSecs(2){
            host = queryHost {
                conditions=["uuid=${host.uuid}"]
            }[0]
            assert host.availableCpuCapacity == -1
        }
    }

}
