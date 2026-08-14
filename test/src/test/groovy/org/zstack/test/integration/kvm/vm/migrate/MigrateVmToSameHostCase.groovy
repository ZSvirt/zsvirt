package org.zstack.test.integration.kvm.vm.migrate

import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMSecurityGroupBackend
import org.zstack.sdk.HostInventory
import org.zstack.sdk.MigrateVmAction
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

/**
 * Created by AlanJager on 2017/5/19.
 */
class MigrateVmToSameHostCase extends SubCase {
    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
        spring {
            ceph()
        }
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

            zone{
                name = "zone"
                cluster {
                    name = "cluster"
                    hypervisorType = "KVM"

                    kvm {
                        name = "ceph-mon"
                        managementIp = "127.0.0.5"
                        username = "root"
                        password = "password"
                        usedMem = 1000
                        totalCpu = 10
                    }
                    kvm {
                        name = "kvm1"
                        managementIp = "127.0.0.1"
                        username = "root"
                        password = "password"
                        usedMem = 1000
                        totalCpu = 10
                    }

                    kvm {
                        name = "kvm2"
                        managementIp = "127.0.0.2"
                        username = "root"
                        password = "password"
                        usedMem = 1000
                        totalCpu = 10
                    }

                    attachPrimaryStorage("ps")
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

                cephPrimaryStorage {
                    name="ps"
                    description="Test"
                    totalCapacity = SizeUnit.GIGABYTE.toByte(100)
                    availableCapacity= SizeUnit.GIGABYTE.toByte(100)
                    url="ceph://pri"
                    fsid="7ff218d9-f525-435f-8a40-3618d1772a64"
                    monUrls=["root:password@localhost/?monPort=7777"]

                }


                attachBackupStorage("bs")
            }

            cephBackupStorage {
                name="bs"
                description="Test"
                totalCapacity = SizeUnit.GIGABYTE.toByte(100)
                availableCapacity= SizeUnit.GIGABYTE.toByte(100)
                url = "/bk"
                fsid ="7ff218d9-f525-435f-8a40-3618d1772a64"
                monUrls = ["root:password@localhost/?monPort=7777"]

                image {
                    name = "test-iso"
                    url  = "http://zstack.org/download/test.iso"
                }
                image {
                    name = "image"
                    url  = "http://zstack.org/download/image.qcow2"
                }
            }

            vm {
                name = "vm1"
                useCluster("cluster")
                useHost("kvm1")
                useL3Networks("l3")
                useInstanceOffering("instanceOffering")
                useImage("image")
            }

            vm {
                name = "vm2"
                useCluster("cluster")
                useHost("kvm1")
                useL3Networks("l3")
                useInstanceOffering("instanceOffering")
                useImage("image")
            }
        }
    }

    @Override
    void test() {
        env.create {
            testMigrateVmAfterMaintainHost()
            testGetMigrationVm()
        }
    }

    void testMigrateVmAfterMaintainHost() {
        VmInstanceInventory vm1 = env.inventoryByName("vm1")
        VmInstanceInventory vm2 = env.inventoryByName("vm2")
        HostInventory host = env.inventoryByName("kvm2")


        env.simulator(KVMSecurityGroupBackend.SECURITY_GROUP_CLEANUP_UNUSED_RULE_ON_HOST_PATH) {
            return new KVMAgentCommands.CleanupUnusedRulesOnHostResponse()
        }

        // do migrate
        migrateVm {
            vmInstanceUuid = vm1.uuid
            hostUuid = host.uuid
        }
        migrateVm {
            vmInstanceUuid = vm2.uuid
            hostUuid = host.uuid
        }

        // confirm migration success
        VmInstanceVO vo1
        VmInstanceVO vo2
        retryInSecs {
            vo1 = dbFindByUuid(vm1.getUuid(), VmInstanceVO.class)
            vo2 = dbFindByUuid(vm2.getUuid(), VmInstanceVO.class)

            assert vo1.lastHostUuid != vo1.hostUuid
            assert vo2.lastHostUuid != vo2.hostUuid
            assert vo1.state == VmInstanceState.Running
            assert vo2.state == VmInstanceState.Running
        }

        // do migrate to the same host again
        MigrateVmAction action = new MigrateVmAction()
        action.hostUuid = host.uuid
        action.vmInstanceUuid = vm1.uuid
        action.sessionId = adminSession()
        MigrateVmAction.Result ret = action.call()

        MigrateVmAction action2 = new MigrateVmAction()
        action2.hostUuid = host.uuid
        action2.vmInstanceUuid = vm1.uuid
        action2.sessionId = adminSession()
        MigrateVmAction.Result ret2 = action2.call()

        assert ret.error != null
        assert ret2.error != null
    }

    void testGetMigrationVm(){
        VmInstanceInventory vm = env.inventoryByName("vm1") as VmInstanceInventory
        HostInventory toDeleteHost1 = env.inventoryByName("ceph-mon") as HostInventory
        HostInventory toDeleteHost2 = env.inventoryByName("kvm1") as HostInventory
        List<HostInventory> hosts = getVmMigrationCandidateHosts {
            vmInstanceUuid = vm.uuid
        } as List<HostInventory>

        assert hosts.size() == 2

        deleteHost {
            uuid = toDeleteHost1.uuid
        }

        deleteHost {
            uuid = toDeleteHost2.uuid
        }

        hosts = getVmMigrationCandidateHosts {
            vmInstanceUuid = vm.uuid
        } as List<HostInventory>

        assert hosts.size() == 0
    }
}
