package org.zstack.test.integration.kvm.vm

import org.springframework.http.HttpEntity
import org.zstack.header.image.ImagePlatform
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.sdk.*
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

class ChangeVmGuestOsCase extends SubCase{
    EnvSpec env

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
                    name = "image-1"
                    url = "http://zstack.org/download/image-1.qcow2"
                    architecture = "aarch64"
                }

                image {
                    name = "image-2"
                    url = "http://zstack.org/download/image-2.qcow2"
                }
            }

            zone {
                name = "zone"
                description = "test"

                cluster {
                    name = "cluster-1"
                    hypervisorType = "KVM"
                    architecture = "aarch64"

                    kvm {
                        name = "kvm-1"
                        managementIp = "127.0.0.1"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("local")
                    attachL2Network("l2")
                }

                cluster {
                    name = "cluster-2"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm-2"
                        managementIp = "127.0.0.2"
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
                            startIp = "12.16.10.10"
                            endIp = "12.16.10.100"
                            netmask = "255.255.255.0"
                            gateway = "12.16.10.1"
                        }
                    }
                }
                attachBackupStorage("sftp")
            }

            vm {
                name = "vm-1"
                useCluster("cluster-1")
                useL3Networks("l3")
                useInstanceOffering("instanceOffering")
                useRootDiskOffering("diskOffering")
                useImage("image-1")
            }

            vm {
                name = "vm-2"
                useCluster("cluster-2")
                useL3Networks("l3")
                useInstanceOffering("instanceOffering")
                useRootDiskOffering("diskOffering")
                useImage("image-2")
            }
        }
    }

    @Override
    void test() {
        env.create {
            changeGuestOsTypeToSetAcpi()
            changeGuestOsTypeToDisableX2apic()
        }
    }

    void changeGuestOsTypeToSetAcpi() {
        def vm = env.inventoryByName("vm-1") as VmInstanceInventory

        KVMAgentCommands.StartVmCmd cmd = null
        env.afterSimulator(KVMConstant.KVM_START_VM_PATH) { rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.StartVmCmd.class)
            return rsp
        }

        rebootVmInstance {
            uuid = vm.uuid
        }
        
        assert cmd.getAcpi()

        updateVmInstance {
            uuid = vm.uuid
            platform = ImagePlatform.Linux.toString()
            guestOsType = "Kylin 4"
        }

        env.afterSimulator(KVMConstant.KVM_START_VM_PATH) { rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.StartVmCmd.class)
            return rsp
        }

        rebootVmInstance {
            uuid = vm.uuid
        }

        assert !cmd.getAcpi()
    }

    void changeGuestOsTypeToDisableX2apic() {
        def vm = env.inventoryByName("vm-2") as VmInstanceInventory

        KVMAgentCommands.StartVmCmd cmd = null
        env.afterSimulator(KVMConstant.KVM_START_VM_PATH) { rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.StartVmCmd.class)
            return rsp
        }

        rebootVmInstance {
            uuid = vm.uuid
        }

        assert cmd.getX2apic()

        updateVmInstance {
            uuid = vm.uuid
            platform = ImagePlatform.Other.toString()
            guestOsType = "Solaris 10"
        }

        env.afterSimulator(KVMConstant.KVM_START_VM_PATH) { rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.StartVmCmd.class)
            return rsp
        }

        rebootVmInstance {
            uuid = vm.uuid
        }

        assert !cmd.getX2apic()
    }

    @Override
    void clean() {
        env.delete()
    }
}
