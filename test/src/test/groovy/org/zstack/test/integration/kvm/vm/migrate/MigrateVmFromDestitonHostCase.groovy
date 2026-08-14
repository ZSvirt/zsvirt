package org.zstack.test.integration.kvm.vm.migrate

import com.google.gson.Gson
import org.springframework.http.HttpEntity
import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.header.Constants
import org.zstack.header.host.HostStatus
import org.zstack.header.host.HostVO
import org.zstack.header.host.HostVO_
import org.zstack.header.longjob.LongJobState
import org.zstack.header.longjob.LongJobVO
import org.zstack.header.longjob.LongJobVO_
import org.zstack.header.vm.APIMigrateVmMsg
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.kvm.KVMGlobalConfig
import org.zstack.kvm.KVMSecurityGroupBackend
import org.zstack.sdk.HostInventory
import org.zstack.sdk.LongJobInventory
import org.zstack.sdk.MigrateVmAction
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

/**
 * Created by shixin.ruan on 2018/02/10.
 */
class MigrateVmFromDestitonHostCase extends SubCase {
    EnvSpec env
    VmInstanceInventory vm1
    def disconnectHostUuid = []

    @Override
    void clean() {
        SQL.New(LongJobVO.class).delete()
        env.delete()
    }

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
        spring {
            ceph()
            include("LongJobManager.xml")
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

            zone {
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
                    name = "ps"
                    description = "Test"
                    totalCapacity = SizeUnit.GIGABYTE.toByte(100)
                    availableCapacity = SizeUnit.GIGABYTE.toByte(100)
                    url = "ceph://pri"
                    fsid = "7ff218d9-f525-435f-8a40-3618d1772a64"
                    monUrls = ["root:password@localhost/?monPort=7777"]

                }


                attachBackupStorage("bs")
            }

            cephBackupStorage {
                name = "bs"
                description = "Test"
                totalCapacity = SizeUnit.GIGABYTE.toByte(100)
                availableCapacity = SizeUnit.GIGABYTE.toByte(100)
                url = "/bk"
                fsid = "7ff218d9-f525-435f-8a40-3618d1772a64"
                monUrls = ["root:password@localhost/?monPort=7777"]

                image {
                    name = "test-iso"
                    url = "http://zstack.org/download/test.iso"
                }
                image {
                    name = "image"
                    url = "http://zstack.org/download/image.qcow2"
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
            vm1 = env.inventoryByName("vm1") as VmInstanceInventory
            prepareEnv()
            testMigrateVmFromDestinationHost()
        }
    }

    void prepareEnv() {
        env.simulator(KVMConstant.KVM_PING_PATH) { HttpEntity<String> e ->
            KVMAgentCommands.PingResponse rsp = new KVMAgentCommands.PingResponse()
            KVMAgentCommands.PingCmd pingCmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.PingCmd.class)
            if (disconnectHostUuid.contains(pingCmd.hostUuid)) {
                rsp.success = false
                rsp.setError("on purpose")
            } else {
                rsp.success = true
            }
            rsp.hostUuid = pingCmd.hostUuid
            return rsp
        }
        env.afterSimulator(KVMConstant.KVM_CONNECT_PATH) { KVMAgentCommands.AgentResponse rsp, HttpEntity<String> e ->
            def cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.ConnectCmd.class)
            if (disconnectHostUuid.contains(cmd.hostUuid)) {
                rsp.success = false
                rsp.setError("on purpose")
            }
            return rsp
        }

        disconnectHostUuid.add(vm1.hostUuid)
        expectError {
            reconnectHost {
                uuid = vm1.hostUuid
            }
        }

    }

    void testMigrateVmFromDestinationHost() {
        HostInventory host1 = env.inventoryByName("kvm1")
        HostInventory host = env.inventoryByName("kvm2")

        env.simulator(KVMSecurityGroupBackend.SECURITY_GROUP_CLEANUP_UNUSED_RULE_ON_HOST_PATH) {
            return new KVMAgentCommands.CleanupUnusedRulesOnHostResponse()
        }

        KVMGlobalConfig.MIGRATE_AUTO_CONVERGE.updateValue(true)
        KVMAgentCommands.MigrateVmCmd cmd = null
        String huuid = null
        env.afterSimulator(KVMConstant.KVM_MIGRATE_VM_PATH) { rsp, HttpEntity<String> entity ->
            huuid = entity.getHeaders().getFirst(Constants.AGENT_HTTP_HEADER_RESOURCE_UUID)
            cmd = json(entity.getBody(), KVMAgentCommands.MigrateVmCmd.class)
            return rsp
        }

        // do migrate
        migrateUnknownVm(host.uuid)

        assert cmd != null
        assert cmd.migrateFromDestination
        assert cmd.autoConverge
        assert cmd.downTime == null
        assert cmd.destHostIp == host.managementIp
        assert cmd.srcHostIp == host1.managementIp
        assert huuid == host.uuid

        // confirm migration success
        VmInstanceVO vo1
        retryInSecs {
            vo1 = dbFindByUuid(vm1.getUuid(), VmInstanceVO.class)

            assert vo1.lastHostUuid != vo1.hostUuid
            assert vo1.state == VmInstanceState.Running
        }

        disconnectHostUuid.clear()
        reconnectHost {
            uuid = vm1.hostUuid
        }

        KVMGlobalConfig.MIGRATE_AUTO_CONVERGE.updateValue(false)
        KVMGlobalConfig.MIGRATE_XBZRLE.updateValue(false)
        cmd = null
        migrateVm {
            vmInstanceUuid = vm1.uuid
            hostUuid = host1.uuid
            strategy = "auto-converge"
            downTime = 300
        }

        assert cmd != null
        assert !cmd.migrateFromDestination
        assert cmd.autoConverge
        assert cmd.downTime != null
        assert !cmd.xbzrle
        assert cmd.destHostIp == host1.managementIp
        assert cmd.srcHostIp == host.managementIp

        retryInSecs {
            vo1 = dbFindByUuid(vm1.getUuid(), VmInstanceVO.class)

            assert vo1.lastHostUuid != vo1.hostUuid
            assert vo1.state == VmInstanceState.Running
        }

        // do migrate to the same host again
        MigrateVmAction action = new MigrateVmAction()
        action.hostUuid = host1.uuid
        action.vmInstanceUuid = vm1.uuid
        action.migrateFromDestination = true
        action.sessionId = adminSession()
        MigrateVmAction.Result ret = action.call()

        assert ret.error != null
    }

    private void migrateUnknownVm(String destHostUuid) {
        if (new Random().nextBoolean()) {
            migrateVm {
                vmInstanceUuid = vm1.uuid
                hostUuid = destHostUuid
                migrateFromDestination = true
                allowUnknown = true
            }
            return
        }

        APIMigrateVmMsg msg = new APIMigrateVmMsg()
        msg.hostUuid = destHostUuid
        msg.vmInstanceUuid = vm1.uuid
        msg.allowUnknown = true
        msg.migrateFromDestination = true

        LongJobInventory jobInv = submitLongJob {
            jobName = msg.getClass().getSimpleName()
            jobData = new Gson().toJson(msg)
        } as LongJobInventory

        assert retryInSecs() {
            return Q.New(LongJobVO.class).eq(LongJobVO_.uuid, jobInv.uuid).select(LongJobVO_.state).findValue() == LongJobState.Succeeded
        }
    }
}
