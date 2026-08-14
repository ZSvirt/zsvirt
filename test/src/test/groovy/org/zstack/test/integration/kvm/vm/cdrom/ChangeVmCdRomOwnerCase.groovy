package org.zstack.test.integration.kvm.vm.cdrom

import org.zstack.compute.vm.VmSystemTags
import org.zstack.header.vm.VmInstanceConstant
import org.zstack.sdk.AccountInventory
import org.zstack.sdk.SessionInventory
import org.zstack.sdk.VmCdRomInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

/**
 * Created by lining on 2019/01/10.
 */
class ChangeVmCdRomOwnerCase extends SubCase {
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
                memory = SizeUnit.GIGABYTE.toByte(1)
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
                    url  = "http://zstack.org/download/image.qcow2"
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
                name = "vm"
                useL3Networks("l3")
                useInstanceOffering("instanceOffering")
                useRootDiskOffering("diskOffering")
                useImage("image")
                systemTags = [
                        "${VmSystemTags.CD_ROM_LIST_TOKEN}::${VmInstanceConstant.EMPTY_CDROM}::${VmInstanceConstant.EMPTY_CDROM}::${VmInstanceConstant.EMPTY_CDROM}".toString()
                ]
            }
        }

    }

    @Override
    void test() {
        env.create {
            changeVmOwnerTest()
        }
    }

    void changeVmOwnerTest() {
        VmInstanceInventory vm = env.inventoryByName("vm")
        assert 3 == vm.vmCdRoms.size()

        String accountPwd = "password"
        AccountInventory account = createAccount {
            name = "test-1"
            password = accountPwd
        }

        changeResourceOwner {
            resourceUuid = vm.uuid
            accountUuid = account.uuid
        }

        SessionInventory sesion = logInByAccount {
            accountName = account.name
            password = accountPwd
        }

        List<VmCdRomInventory> cdRomInventories = queryVmCdRom {
            conditions = [
                    "vmInstanceUuid=${vm.uuid}"
            ]
            sessionId : sesion.uuid
        }
        assert 3 == cdRomInventories.size()
    }
}