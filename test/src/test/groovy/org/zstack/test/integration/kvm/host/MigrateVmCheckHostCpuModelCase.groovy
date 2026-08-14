package org.zstack.test.integration.kvm.host

import org.zstack.kvm.KVMGlobalConfig
import org.zstack.kvm.KVMSystemTags
import org.zstack.sdk.HostInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

import static org.zstack.kvm.KVMConstant.*
import static org.zstack.kvm.KVMAgentCommands.*
import static org.zstack.utils.CollectionDSL.e
import static org.zstack.utils.CollectionDSL.map

/**
 * Created by kayo on 2018/3/13.
 */
class MigrateVmCheckHostCpuModelCase extends SubCase {
    EnvSpec env
    def cpuModelName1 = "model1"
    def cpuModelName2 = "model2"

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
                    name = "image"
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
                        name = "host1"
                        managementIp = "localhost"
                        username = "root"
                        password = "password"
                        systemTags = [
                                KVMSystemTags.QEMU_IMG_VERSION.instantiateTag(map(e(KVMSystemTags.QEMU_IMG_VERSION_TOKEN, "2.6.0"))),
                                KVMSystemTags.LIBVIRT_VERSION.instantiateTag(map(e(KVMSystemTags.LIBVIRT_VERSION_TOKEN, "1.3.3"))),
                                KVMSystemTags.CPU_MODEL_NAME.instantiateTag(map(e(KVMSystemTags.CPU_MODEL_NAME_TOKEN, cpuModelName1)))
                        ]
                    }

                    kvm {
                        name = "host2"
                        username = "root"
                        password = "password"
                        systemTags = [
                                KVMSystemTags.QEMU_IMG_VERSION.instantiateTag(map(e(KVMSystemTags.QEMU_IMG_VERSION_TOKEN, "2.6.0"))),
                                KVMSystemTags.LIBVIRT_VERSION.instantiateTag(map(e(KVMSystemTags.LIBVIRT_VERSION_TOKEN, "1.3.3"))),
                                KVMSystemTags.CPU_MODEL_NAME.instantiateTag(map(e(KVMSystemTags.CPU_MODEL_NAME_TOKEN, cpuModelName2)))
                        ]
                    }

                    attachPrimaryStorage("nfs")
                    attachL2Network("l2")
                }

                nfsPrimaryStorage {
                    name = "nfs"
                    url = "localhost:/nfs_ps"
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
                useInstanceOffering("instanceOffering")
                useImage("image")
                useL3Networks("l3")
                useRootDiskOffering("diskOffering")
                useHost("host1")
            }
        }
    }

    @Override
    void test() {
        env.afterSimulator(KVM_HOST_FACT_PATH) { HostFactResponse rsp ->
            rsp.qemuImgVersion = ""
            rsp.libvirtVersion = ""
            rsp.cpuModelName = ""
            return rsp
        }

        env.create {
            testVmMigrateCheckHostCpuModel()
        }
    }

    void testVmMigrateCheckHostCpuModel() {
        VmInstanceInventory vm = env.inventoryByName("vm") as VmInstanceInventory
        HostInventory host2 = env.inventoryByName("host2") as HostInventory

        // if check host cpu model name no hosts can be candidates due to different cpu model name
        updateGlobalConfig {
            category = KVMGlobalConfig.CATEGORY
            name = "checkHostCpuModelName"
            value = true
        }

        def list = getVmMigrationCandidateHosts {
            vmInstanceUuid = vm.uuid
        } as List<HostInventory>

        assert list.isEmpty()

        updateGlobalConfig {
            category = KVMGlobalConfig.CATEGORY
            name = "checkHostCpuModelName"
            value = false
        }

        list = getVmMigrationCandidateHosts {
            vmInstanceUuid = vm.uuid
        } as List<HostInventory>

        assert list.size() == 1
        assert list.get(0).uuid == host2.uuid
    }
}
