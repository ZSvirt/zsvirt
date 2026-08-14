package org.zstack.test.integration.kvm.host

import org.zstack.header.host.HostVO
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

class MigrateVmCheckKvmPropertyCase extends SubCase {
    EnvSpec env
    def cpuModelName = "model"
    def libvirtVersion = "4.9.0"
    def qemuImgVersion = "4.2.0"
    def ept = "ept"
    HostInventory host2
    VmInstanceInventory vm1, vm2

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
                                KVMSystemTags.QEMU_IMG_VERSION.instantiateTag(map(e(KVMSystemTags.QEMU_IMG_VERSION_TOKEN, qemuImgVersion))),
                                KVMSystemTags.LIBVIRT_VERSION.instantiateTag(map(e(KVMSystemTags.LIBVIRT_VERSION_TOKEN, libvirtVersion))),
                                KVMSystemTags.CPU_MODEL_NAME.instantiateTag(map(e(KVMSystemTags.CPU_MODEL_NAME_TOKEN, cpuModelName))),
                                KVMSystemTags.EPT_CPU_FLAG.instantiateTag(map(e(KVMSystemTags.EPT_CPU_FLAG_TOKEN, ept)))
                        ]
                    }

                    attachPrimaryStorage("nfs")
                    attachL2Network("l2")
                }

                cluster {
                    name = "cluster2"
                    hypervisorType = "KVM"

                    kvm {
                        name = "host2"
                        username = "root"
                        password = "password"
                        systemTags = [
                                KVMSystemTags.QEMU_IMG_VERSION.instantiateTag(map(e(KVMSystemTags.QEMU_IMG_VERSION_TOKEN, qemuImgVersion))),
                                KVMSystemTags.LIBVIRT_VERSION.instantiateTag(map(e(KVMSystemTags.LIBVIRT_VERSION_TOKEN, libvirtVersion))),
                                KVMSystemTags.CPU_MODEL_NAME.instantiateTag(map(e(KVMSystemTags.CPU_MODEL_NAME_TOKEN, cpuModelName))),
                                KVMSystemTags.EPT_CPU_FLAG.instantiateTag(map(e(KVMSystemTags.EPT_CPU_FLAG_TOKEN, ept)))
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

            vm {
                name = "vm2"
                useInstanceOffering("instanceOffering")
                useImage("image")
                useL3Networks("l3")
                useRootDiskOffering("diskOffering")
                useHost("host2")
            }
        }
    }

    @Override
    void test() {
        env.afterSimulator(KVM_HOST_FACT_PATH) { HostFactResponse rsp ->
            rsp.eptFlag = ""
            rsp.qemuImgVersion = ""
            rsp.libvirtVersion = ""
            rsp.cpuModelName = ""
            return rsp
        }

        env.create {
            vm1 = env.inventoryByName("vm") as VmInstanceInventory
            vm2 = env.inventoryByName("vm2") as VmInstanceInventory
            host2 = env.inventoryByName("host2") as HostInventory
            testHostTokenMismatch()
            testHostTokenNotExists()
        }
    }

    String mockWrongValue(String input) {
        return "${input}-wrong"
    }

    void confirmMigrateAvailable(VmInstanceInventory vm) {
        confirmMigrationAvailability(vm, true)
    }

    void confirmMigrationAvailability(VmInstanceInventory vm, boolean expectedAvailable) {
        def list = getVmMigrationCandidateHosts {
            vmInstanceUuid = vm.uuid
        } as List<HostInventory>

        if (expectedAvailable) {
            assert list.size() == 1
            assert list.get(0).uuid != vm.hostUuid
        } else {
            assert list.size() == 0
        }
    }

    void confirmMigrateUnavailable(VmInstanceInventory vm) {
        confirmMigrationAvailability(vm, false)
    }

    void testHostTokenMismatch() {
        // initial kvm host property is same, expected both vm can migrate
        confirmMigrateAvailable(vm1)
        confirmMigrateAvailable(vm2)

        // change qemu-img version to different value, both vms expected can not be migrated
        KVMSystemTags.QEMU_IMG_VERSION.updateTagByToken(host2.uuid, KVMSystemTags.QEMU_IMG_VERSION_TOKEN, mockWrongValue(qemuImgVersion))
        confirmMigrateUnavailable(vm1)
        confirmMigrateUnavailable(vm2)

        // change qemu-img version back confirm can migrate again
        KVMSystemTags.QEMU_IMG_VERSION.updateTagByToken(host2.uuid, KVMSystemTags.QEMU_IMG_VERSION_TOKEN, qemuImgVersion)
        confirmMigrateAvailable(vm1)
        confirmMigrateAvailable(vm2)

        // change libvirt version to different value, both vms expected can not be migrated
        KVMSystemTags.LIBVIRT_VERSION.updateTagByToken(host2.uuid, KVMSystemTags.LIBVIRT_VERSION_TOKEN, mockWrongValue(libvirtVersion))
        confirmMigrateUnavailable(vm1)
        confirmMigrateUnavailable(vm2)

        // change libvirt version back confirm can migrate again
        KVMSystemTags.LIBVIRT_VERSION.updateTagByToken(host2.uuid, KVMSystemTags.LIBVIRT_VERSION_TOKEN, libvirtVersion)
        confirmMigrateAvailable(vm1)
        confirmMigrateAvailable(vm2)

        // change cpu model to different value,
        // both vms can be migrated before checkHostCpuModelName global config enabled
        KVMSystemTags.CPU_MODEL_NAME.updateTagByToken(host2.uuid, KVMSystemTags.CPU_MODEL_NAME_TOKEN, mockWrongValue(cpuModelName))
        confirmMigrateAvailable(vm1)
        confirmMigrateAvailable(vm2)

        // enable global config and confirm vms can not be migrated
        updateGlobalConfig {
            category = KVMGlobalConfig.CATEGORY
            name = "checkHostCpuModelName"
            value = true
        }
        confirmMigrateUnavailable(vm1)
        confirmMigrateUnavailable(vm2)

        // change cpu model back confirm can migrate again
        KVMSystemTags.CPU_MODEL_NAME.updateTagByToken(host2.uuid, KVMSystemTags.CPU_MODEL_NAME_TOKEN, cpuModelName)
        confirmMigrateAvailable(vm1)
        confirmMigrateAvailable(vm2)

        // disable global config and confirm vms still can be migrated
        updateGlobalConfig {
            category = KVMGlobalConfig.CATEGORY
            name = "checkHostCpuModelName"
            value = false
        }
        confirmMigrateAvailable(vm1)
        confirmMigrateAvailable(vm2)

        // change ept to different value, both vms expected can not be migrated
        KVMSystemTags.EPT_CPU_FLAG.delete(host2.uuid)
        createSystemTag {
            resourceUuid = host2.uuid
            resourceType = HostVO.simpleName
            tag = "ept::${mockWrongValue(ept)}"
        }
        confirmMigrateUnavailable(vm1)
        confirmMigrateUnavailable(vm2)

        // change ept back confirm can migrate again
        KVMSystemTags.EPT_CPU_FLAG.updateTagByToken(host2.uuid, KVMSystemTags.EPT_CPU_FLAG_TOKEN, ept)
        confirmMigrateAvailable(vm1)
        confirmMigrateAvailable(vm2)
    }

    void testHostTokenNotExists() {
        confirmMigrateAvailable(vm1)
        confirmMigrateAvailable(vm2)

        KVMSystemTags.QEMU_IMG_VERSION.delete(host2.uuid)
        // vm1 not allowed to migrate to host2 without qemu-img tag
        confirmMigrateUnavailable(vm1)
        // vm2 is allowed to migrate to host1 because current host qemu-img version is not available
        confirmMigrateUnavailable(vm2)
        // create qemu-img tag
        createSystemTag {
            resourceUuid = host2.uuid
            resourceType = HostVO.simpleName
            tag = "qemu-img::version::${qemuImgVersion}"
        }
        confirmMigrateAvailable(vm1)
        confirmMigrateAvailable(vm2)

        KVMSystemTags.LIBVIRT_VERSION.delete(host2.uuid)
        // vm1 not allowed to migrate to host2 without libvirt version tag
        confirmMigrateUnavailable(vm1)
        // vm2 is allowed to migrate to host1 because current host libvirt version is not available
        confirmMigrateUnavailable(vm2)
        createSystemTag {
            resourceUuid = host2.uuid
            resourceType = HostVO.simpleName
            tag = "libvirt::version::${libvirtVersion}"
        }
        confirmMigrateAvailable(vm1)
        confirmMigrateAvailable(vm2)

        updateGlobalConfig {
            category = KVMGlobalConfig.CATEGORY
            name = "checkHostCpuModelName"
            value = true
        }
        KVMSystemTags.CPU_MODEL_NAME.delete(host2.uuid)
        // vm1 not allowed to migrate to host2 without cpu model tag
        confirmMigrateUnavailable(vm1)
        // vm2 is allowed to migrate to host1 because current host cpu model is not available
        confirmMigrateUnavailable(vm2)
        createSystemTag {
            resourceUuid = host2.uuid
            resourceType = HostVO.simpleName
            tag = "cpuModelName::${cpuModelName}"
        }
        updateGlobalConfig {
            category = KVMGlobalConfig.CATEGORY
            name = "checkHostCpuModelName"
            value = false
        }
        confirmMigrateAvailable(vm1)
        confirmMigrateAvailable(vm2)

        KVMSystemTags.EPT_CPU_FLAG.delete(host2.uuid)
        // vm1 not allowed to migrate to host2 without ept tag
        confirmMigrateUnavailable(vm1)
        // vm2 is allowed to migrate to host1 because current host ept is not available
        confirmMigrateUnavailable(vm2)
        createSystemTag {
            resourceUuid = host2.uuid
            resourceType = HostVO.simpleName
            tag = "ept::${ept}"
        }
        confirmMigrateAvailable(vm1)
        confirmMigrateAvailable(vm2)
    }
}

