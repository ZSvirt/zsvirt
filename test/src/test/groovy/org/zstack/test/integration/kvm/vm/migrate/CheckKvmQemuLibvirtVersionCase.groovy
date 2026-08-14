package org.zstack.test.integration.kvm.vm.migrate

import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMSecurityGroupBackend
import org.zstack.kvm.KVMSystemTags
import org.zstack.sdk.ClusterInventory
import org.zstack.sdk.HostInventory
import org.zstack.sdk.MigrateVmAction
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

import static org.zstack.kvm.KVMConstant.*
import static org.zstack.kvm.KVMAgentCommands.*
import static org.zstack.utils.CollectionDSL.e
import static org.zstack.utils.CollectionDSL.map

/**
 * Created by GuoYi on 2018/1/12.
 */
class CheckKvmQemuLibvirtVersionCase extends SubCase {
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
                    name = "cluster-1"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm11"
                        managementIp = "127.0.0.11"
                        username = "root"
                        password = "password"
                        systemTags = [
                                KVMSystemTags.QEMU_IMG_VERSION.instantiateTag(map(e(KVMSystemTags.QEMU_IMG_VERSION_TOKEN, "2.6.0"))),
                                KVMSystemTags.LIBVIRT_VERSION.instantiateTag(map(e(KVMSystemTags.LIBVIRT_VERSION_TOKEN, "1.3.3")))
                        ]
                    }

                    kvm {
                        name = "kvm12"
                        managementIp = "127.0.0.12"
                        username = "root"
                        password = "password"
                        systemTags = [
                                KVMSystemTags.QEMU_IMG_VERSION.instantiateTag(map(e(KVMSystemTags.QEMU_IMG_VERSION_TOKEN, "2.6.0"))),
                                KVMSystemTags.LIBVIRT_VERSION.instantiateTag(map(e(KVMSystemTags.LIBVIRT_VERSION_TOKEN, "1.3.3")))
                        ]
                    }

                    kvm {
                        name = "kvm13"
                        managementIp = "127.0.0.13"
                        username = "root"
                        password = "password"
                        systemTags = [
                                KVMSystemTags.QEMU_IMG_VERSION.instantiateTag(map(e(KVMSystemTags.QEMU_IMG_VERSION_TOKEN, "2.6.0"))),
                                KVMSystemTags.LIBVIRT_VERSION.instantiateTag(map(e(KVMSystemTags.LIBVIRT_VERSION_TOKEN, "1.3.3")))
                        ]
                    }

                    attachPrimaryStorage("nfs")
                    attachL2Network("l2-1")
                }

                cluster {
                    name = "cluster-2"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm21"
                        managementIp = "127.0.0.21"
                        username = "root"
                        password = "password"
                        systemTags = [
                                KVMSystemTags.QEMU_IMG_VERSION.instantiateTag(map(e(KVMSystemTags.QEMU_IMG_VERSION_TOKEN, "2.9.0"))),
                                KVMSystemTags.LIBVIRT_VERSION.instantiateTag(map(e(KVMSystemTags.LIBVIRT_VERSION_TOKEN, "1.3.3")))
                        ]
                    }

                    kvm {
                        name = "kvm22"
                        managementIp = "127.0.0.22"
                        username = "root"
                        password = "password"
                        systemTags = [
                                KVMSystemTags.QEMU_IMG_VERSION.instantiateTag(map(e(KVMSystemTags.QEMU_IMG_VERSION_TOKEN, "2.9.0"))),
                                KVMSystemTags.LIBVIRT_VERSION.instantiateTag(map(e(KVMSystemTags.LIBVIRT_VERSION_TOKEN, "1.3.3")))
                        ]
                    }

                    attachPrimaryStorage("nfs")
                    attachL2Network("l2-1")
                }

                cluster {
                    name = "cluster-3"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm31"
                        managementIp = "127.0.0.31"
                        username = "root"
                        password = "password"
                        systemTags = [
                                KVMSystemTags.QEMU_IMG_VERSION.instantiateTag(map(e(KVMSystemTags.QEMU_IMG_VERSION_TOKEN, "2.6.0"))),
                                KVMSystemTags.LIBVIRT_VERSION.instantiateTag(map(e(KVMSystemTags.LIBVIRT_VERSION_TOKEN, "1.3.3")))
                        ]
                    }

                    attachPrimaryStorage("nfs")
                    attachL2Network("l2-1")
                }

                nfsPrimaryStorage {
                    name = "nfs"
                    url = "localhost:/nfs"
                }

                l2NoVlanNetwork {
                    name = "l2-1"
                    physicalInterface = "eth0"

                    l3Network {
                        name = "l3-1"

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
                useL3Networks("l3-1")
                useCluster("cluster-1")
                useHost("kvm11")
            }
        }
    }

    @Override
    void test() {
        env.afterSimulator(KVM_HOST_FACT_PATH) { HostFactResponse rsp ->
            rsp.qemuImgVersion = ""
            rsp.libvirtVersion = ""
            return rsp
        }

        env.create {
            testCheckKvmQemuLibvirtVersion()
            testGetVmMigrationCandidateHosts()
            testVmMigrationHostMaintain()
        }
    }

    void testCheckKvmQemuLibvirtVersion() {
        VmInstanceInventory vm = env.inventoryByName("vm")
        HostInventory host11 = env.inventoryByName("kvm11")
        HostInventory host12 = env.inventoryByName("kvm12")
        HostInventory host21 = env.inventoryByName("kvm21")
        HostInventory host31 = env.inventoryByName("kvm31")

        env.simulator(KVMSecurityGroupBackend.SECURITY_GROUP_CLEANUP_UNUSED_RULE_ON_HOST_PATH) {
            return new KVMAgentCommands.CleanupUnusedRulesOnHostResponse()
        }

        // host11 and host12 version match
        migrateVm {
            vmInstanceUuid = vm.uuid
            hostUuid = host12.uuid
        }

        // confirm migration success
        retryInSecs {
            def vmVO = dbFindByUuid(vm.getUuid(), VmInstanceVO.class)
            assert vmVO.lastHostUuid == host11.uuid
            assert vmVO.hostUuid == host12.uuid
            assert vmVO.state == VmInstanceState.Running
        }

        // host12 and host21 version not match
        MigrateVmAction action = new MigrateVmAction()
        action.vmInstanceUuid = vm.uuid
        action.hostUuid = host21.uuid
        action.sessionId = adminSession()
        MigrateVmAction.Result ret = action.call()
        assert ret.error != null

        // host12 and host31 version match
        migrateVm {
            vmInstanceUuid = vm.uuid
            hostUuid = host31.uuid
        }

        // confirm migration success
        retryInSecs {
            def vmVO = dbFindByUuid(vm.getUuid(), VmInstanceVO.class)
            assert vmVO.lastHostUuid == host12.uuid
            assert vmVO.hostUuid == host31.uuid
            assert vmVO.state == VmInstanceState.Running
        }
    }

    void testGetVmMigrationCandidateHosts() {
        VmInstanceInventory vm = env.inventoryByName("vm") as VmInstanceInventory
        HostInventory host22 = env.inventoryByName("kvm22") as HostInventory

        List<HostInventory> hosts = getVmMigrationCandidateHosts {
            vmInstanceUuid = vm.uuid
        } as List<HostInventory>

        // host11, host12, host13, host31 have same version
        assert hosts.size() == 3

        deleteHost {
            uuid = host22.uuid
        }

        hosts = getVmMigrationCandidateHosts {
            vmInstanceUuid = vm.uuid
        } as List<HostInventory>

        // host22 is not a migration candidate
        assert hosts.size() == 3
    }

    void testVmMigrationHostMaintain() {
        ClusterInventory cls1 = env.inventoryByName("cluster-1")
        ClusterInventory cls3 = env.inventoryByName("cluster-3")
        HostInventory host31 = env.inventoryByName("kvm31")
        VmInstanceInventory vm = env.inventoryByName("vm")

        // make sure vm is in cluster-3
        def vmVO = dbFindByUuid(vm.getUuid(), VmInstanceVO.class)
        assert vmVO.clusterUuid == cls3.uuid
        assert vmVO.hostUuid == host31.uuid

        changeHostState {
            uuid = host31.uuid
            stateEvent = "maintain"
        }

        // make sure vm migrated to cluster-1, not cluster-2
        retryInSecs() {
            vmVO = dbFindByUuid(vm.getUuid(), VmInstanceVO.class)
            assert vmVO.clusterUuid == cls1.uuid
        }
    }
}
