package org.zstack.test.integration.platforms.ha.config

import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.ha.HaScheduler
import org.zstack.ha.entity.VmHaVO
import org.zstack.ha.entity.VmHaVO_
import org.zstack.ha.header.VmHaLevel
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.vm.VmSchedHistoryVO
import org.zstack.header.vm.VmSchedHistoryVO_
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.ZStackTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

class HaNeverStopCase extends SubCase {
    EnvSpec env

    @Override
    void setup() {
        useSpring(ZStackTest.springSpec)
    }

    @Override
    void environment() {
        env = makeEnv {
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

                    attachPrimaryStorage("nfs")
                    attachL2Network("l2")
                }

                nfsPrimaryStorage {
                    name = "nfs"
                    url = "localhost:/nfs"
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

                    l3Network {
                        name = "pubL3"

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
                cpu = 4
                memoryGB(8)
                useHost("kvm2")
                useImage("image1")
                useL3Networks("l3")
            }
        }
    }

    @Override
    void test() {
        env.create {
            testAddNeverStopToStoppedVm()
        }
    }

    /**
     * from cloud: HaCase.testAddNeverStopToStoppedVm
     */
    void testAddNeverStopToStoppedVm() {
        def vmInstanceInventory = env.inventoryByName("vm") as VmInstanceInventory
        stopVmInstance {
            delegate.uuid = vmInstanceInventory.uuid
        }

        def vo = dbFindByUuid(vmInstanceInventory.uuid, VmInstanceVO)
        assert vo.state.toString() == VmInstanceState.Stopped.toString()

        logger.info("Test 001: vm.ha None -> NeverStop")
        setVmInstanceHaLevel {
            delegate.uuid = vmInstanceInventory.uuid
            delegate.level = VmHaLevel.NeverStop.toString()
        }

        def result001 = zqlQuery("query VmHa where uuid = '${vmInstanceInventory.uuid}'".toString())
        assert result001.size() == 1
        assert result001[0] instanceof Map
        assert result001[0]["uuid"] == vmInstanceInventory.uuid
        assert result001[0]["haLevel"] == VmHaLevel.NeverStop.toString()
        assert result001[0]["haLevelUpdateTime"]
        assert result001[0]["inhibitionReason"] == "Enable HA for Stopped VM"
        assert result001[0]["inhibitionTime"]
        assert result001[0]["lastFencerName"] == null

        def vm001 = queryVmInstance {
            delegate.conditions = ["uuid=${vmInstanceInventory.uuid}"]
        } as List<VmInstanceInventory>
        assert vm001.size() == 1
        assert vm001[0].state == VmInstanceState.Stopped.toString()

        logger.info("Note: if VM is stopped, update HA level will NOT boot VM, VM is still Stopped")
        def error001 = threadFacadeForTest().triggerBlockedPeriodicTask(HaScheduler.TASK_NAME_STOPPED_VM_SCANNER)
        assert error001 == null

        vm001 = queryVmInstance {
            delegate.conditions = ["uuid=${vmInstanceInventory.uuid}"]
        } as List<VmInstanceInventory>
        assert vm001.size() == 1
        assert vm001[0].state == VmInstanceState.Stopped.toString()
        assert !Q.New(VmSchedHistoryVO.class)
                .eq(VmSchedHistoryVO_.vmInstanceUuid, vmInstanceInventory.uuid)
                .isExists()

        logger.info("Test 002: mock: clean VmHaVO.inhibitionReason")

        SQL.New(VmHaVO.class)
                .eq(VmHaVO_.uuid, vmInstanceInventory.uuid)
                .set(VmHaVO_.inhibitionReason, null)
                .update()

        def error002 = threadFacadeForTest().triggerBlockedPeriodicTask(HaScheduler.TASK_NAME_STOPPED_VM_SCANNER)
        assert error002 == null

        retryInSecs {
            def vm002 = queryVmInstance {
                delegate.conditions = ["uuid=${vmInstanceInventory.uuid}"]
            } as List<VmInstanceInventory>
            assert vm002.size() == 1
            assert vm002[0].state == VmInstanceState.Running.toString()
        }

        assert Q.New(VmSchedHistoryVO.class)
                .eq(VmSchedHistoryVO_.vmInstanceUuid, vmInstanceInventory.uuid)
                .isExists()
        SQL.New(VmSchedHistoryVO.class).delete()

        logger.info("Test 003: vm.ha NeverStop -> None")

        stopVmInstance {
            delegate.uuid = vmInstanceInventory.uuid
        }

        deleteVmInstanceHaLevel {
            delegate.uuid = vmInstanceInventory.uuid
        }

        def result003 = zqlQuery("query VmHa where uuid = '${vmInstanceInventory.uuid}'".toString())
        assert result003.size() == 1
        assert result003[0] instanceof Map
        assert result003[0]["uuid"] == vmInstanceInventory.uuid
        assert result003[0]["haLevel"] == VmHaLevel.None.toString()
        assert result003[0]["haLevelUpdateTime"]
        assert result003[0]["inhibitionReason"] == null
        assert result003[0]["lastFencerName"] == null

        def error003 = threadFacadeForTest().triggerBlockedPeriodicTask(HaScheduler.TASK_NAME_STOPPED_VM_SCANNER)
        assert error003 == null

        def vm003 = queryVmInstance {
            delegate.conditions = ["uuid=${vmInstanceInventory.uuid}"]
        } as List<VmInstanceInventory>
        assert vm003.size() == 1
        assert vm003[0].state == VmInstanceState.Stopped.toString()

        logger.info("Test 009: clean")
        SQL.New(VmSchedHistoryVO.class).delete()
    }

    @Override
    void clean() {
        env.delete()
    }
}
