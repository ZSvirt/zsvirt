package org.zstack.test.integration.platforms.vmsched

import org.zstack.compute.vm.VmSchedHistoryRecorder
import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.db.SQL
import org.zstack.ha.header.VmHaLevel
import org.zstack.header.host.HostStateEvent
import org.zstack.header.vm.MigrateVmMsg
import org.zstack.header.vm.MigrateVmReply
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.vm.VmSchedHistoryVO
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VmSchedHistoryInventory
import org.zstack.test.integration.ZStackTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

class MigrateVmWithNfsPsCase extends SubCase {
    EnvSpec env

    VmInstanceInventory vm

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
            prepare()
            testMigrateVmWhenHostMaintenance()
        }
    }

    void prepare() {
        vm = env.inventoryByName("vm") as VmInstanceInventory

        setVmInstanceHaLevel {
            uuid = vm.uuid
            level = VmHaLevel.NeverStop.toString()
        }
    }

    /**
     * from cloud: HaCase.testWhenHostDownVmWillStartAtAnotherHostOnNfsPrimaryStorage
     */
    void testMigrateVmWhenHostMaintenance() {
        def history000 = queryVmSchedHistory {} as List<VmSchedHistoryInventory>
        assert history000.empty

        logger.info("Test 001: testMigrateVmWhenHostMaintenance")
        logger.info("the env use nfs primary storage, so vm will migrate when host Maintenance")

        def vm = dbFindByUuid(vm.uuid, VmInstanceVO.class)
        boolean flag = false
        env.message(MigrateVmMsg.class) { MigrateVmMsg msg, CloudBus bus ->
            if (msg.vmInstanceUuid == vm.uuid) {
                flag = true
            }
            bus.reply(msg, new MigrateVmReply())
        }

        changeHostState {
            uuid = vm.hostUuid
            stateEvent = HostStateEvent.maintain.toString()
        }

        retryInSecs {
            assert flag
        }

        env.cleanSimulatorAndMessageHandlers()

        def history001 = queryVmSchedHistory {} as List<VmSchedHistoryInventory>
        assert history001.size() == 1
        assert history001[0].vmInstanceUuid == vm.uuid
        assert history001[0].schedType == VmSchedHistoryRecorder.TYPE_HOST_MAINTENANCE

        logger.info("Test 009: clean")
        SQL.New(VmSchedHistoryVO).delete()
    }

    @Override
    void clean() {
        env.delete()
    }
}
