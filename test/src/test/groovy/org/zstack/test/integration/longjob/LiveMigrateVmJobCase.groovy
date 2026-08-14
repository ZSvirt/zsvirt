package org.zstack.test.integration.longjob

import com.google.gson.Gson
import org.springframework.http.HttpEntity
import org.zstack.core.agent.AgentConstant
import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.header.allocator.AllocateHostMsg
import org.zstack.header.allocator.AllocateHostReply
import org.zstack.header.allocator.HostCapacityVO
import org.zstack.header.allocator.HostCapacityVO_
import org.zstack.header.host.HostVO
import org.zstack.header.host.HostVO_
import org.zstack.header.longjob.LongJobConstants
import org.zstack.header.longjob.LongJobVO
import org.zstack.header.longjob.LongJobVO_
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.header.vm.APIMigrateVmMsg
import org.zstack.header.vm.VmInstanceVO
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.kvm.KVMSecurityGroupBackend
import org.zstack.network.securitygroup.SecurityGroupConstant
import org.zstack.network.service.virtualrouter.VirtualRouterConstant
import org.zstack.sdk.*
import org.zstack.test.integration.ZStackTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

/**
 * Created by camile on 18-3-7.
 */
class LiveMigrateVmJobCase extends SubCase {
    EnvSpec env
    DatabaseFacade dbf
    Gson gson
    VmInstanceInventory vm1
    KVMHostInventory host1
    KVMHostInventory host2
    InstanceOfferingInventory instance

    @Override
    void clean() {
        SQL.New(LongJobVO.class).delete()
        env.delete()
    }

    @Override
    void setup() {
        useSpring(ZStackTest.springSpec)
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
                    name = "test-iso1"
                    url = "http://zstack.org/download/test.iso"
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
                    name = "cluster"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm"
                        managementIp = "localhost"
                        username = "root"
                        password = "password"
                    }

                    kvm {
                        name = "kvm1"
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

                        service {
                            provider = VirtualRouterConstant.PROVIDER_TYPE
                            types = [NetworkServiceType.DHCP.toString(), NetworkServiceType.DNS.toString()]
                        }

                        service {
                            provider = SecurityGroupConstant.SECURITY_GROUP_PROVIDER_TYPE
                            types = [SecurityGroupConstant.SECURITY_GROUP_NETWORK_SERVICE_TYPE]
                        }

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

                virtualRouterOffering {
                    name = "vr"
                    memory = SizeUnit.MEGABYTE.toByte(512)
                    cpu = 2
                    useManagementL3Network("pubL3")
                    usePublicL3Network("pubL3")
                    useImage("vr")
                }

                attachBackupStorage("sftp")
            }

            vm {
                name = "vm"
                useInstanceOffering("instanceOffering")
                useImage("image1")
                useL3Networks("l3")
                useRootDiskOffering("diskOffering")
                useHost("kvm")
            }

        }
    }

    @Override
    void test() {
        env.create {
            instance = env.inventoryByName("instanceOffering") as InstanceOfferingInventory
            testLiveMigrateVmLongJobFailure()
            testLiveMigrateVmLongJobSuccess()
            testLiveMigrateVmLongJobCancel()
            testLiveMigrateVmLongJobCancelFail()
            testLiveMigrateVmLongJobCancelBeforeMigrate()
            testLiveMigrateVmNoJobToCancel()
        }
    }

    void testLiveMigrateVmLongJobFailure() {
        dbf = bean(DatabaseFacade.class)
        gson = new Gson()
        vm1 = env.inventoryByName("vm") as VmInstanceInventory
        host1 = env.inventoryByName("kvm") as KVMHostInventory
        host2 = env.inventoryByName("kvm1") as KVMHostInventory
        env.simulator(KVMSecurityGroupBackend.SECURITY_GROUP_CLEANUP_UNUSED_RULE_ON_HOST_PATH) {
            return new KVMAgentCommands.CleanupUnusedRulesOnHostResponse()
        }

        assert vm1.hostUuid == host1.uuid


        APIMigrateVmMsg msg = new APIMigrateVmMsg()
        msg.hostUuid = host2.uuid + 1
        msg.vmInstanceUuid = vm1.uuid

        expect(AssertionError.class) {
            submitLongJob {
                jobName = msg.getClass().getSimpleName()
                jobData = gson.toJson(msg)
            }
        }
    }

    void testLiveMigrateVmLongJobSuccess() {
        APIMigrateVmMsg msg = new APIMigrateVmMsg()
        msg.hostUuid = host2.uuid
        msg.vmInstanceUuid = vm1.uuid

        LongJobInventory jobInv = submitLongJob {
            jobName = msg.getClass().getSimpleName()
            jobData = gson.toJson(msg)
        } as LongJobInventory

        assert jobInv.jobName == msg.getClass().getSimpleName()
        assert jobInv.state == LongJobState.Running

        retryInSecs() {
            VmInstanceVO vo = dbf.findByUuid(vm1.uuid, VmInstanceVO.class)
            assert host2.uuid == vo.hostUuid
        }
        retryInSecs() {
            LongJobVO job = dbFindByUuid(jobInv.getUuid(), LongJobVO.class)
            assert job.state.toString() == LongJobState.Succeeded.toString()
        }

    }

    void testLiveMigrateVmLongJobCancel() {
        APIMigrateVmMsg msg = new APIMigrateVmMsg()
        msg.hostUuid = host1.uuid
        msg.vmInstanceUuid = vm1.uuid
        def canceled = false
        def migrating = false

        env.simulator(KVMConstant.KVM_MIGRATE_VM_PATH) { HttpEntity<String> e ->
            migrating = true
            while (!canceled) {
                sleep(500)
            }
            def rsp = new KVMAgentCommands.MigrateVmResponse()
            rsp.setError("canceled")
            return rsp
        }

        env.simulator(AgentConstant.CANCEL_JOB) {
            canceled = true
            return new KVMAgentCommands.CancelRsp()
        }

        env.simulator(KVMConstant.KVM_VM_CHECK_STATE) {
            KVMAgentCommands.CheckVmStateRsp rsp = new KVMAgentCommands.CheckVmStateRsp()
            rsp.states = [:]
            rsp.states.put(vm1.uuid, KVMConstant.KvmVmState.Running.toString())
            return rsp
        }

        LongJobInventory jobInv = submitLongJob {
            jobName = msg.getClass().getSimpleName()
            jobData = gson.toJson(msg)
        } as LongJobInventory

        assert jobInv.jobName == msg.getClass().getSimpleName()
        assert jobInv.state == LongJobState.Running

        while (!migrating) {
            sleep(500)
        }
        cancelLongJob {
            uuid = jobInv.uuid
        }

        retryInSecs() {
            LongJobVO job = dbFindByUuid(jobInv.getUuid(), LongJobVO.class)
            assert job.state.toString() == LongJobState.Canceled.toString()
        }
        assert host2.uuid == dbf.findByUuid(vm1.uuid, VmInstanceVO.class).hostUuid
        assert canceled
    }

    void testLiveMigrateVmNoJobToCancel() {
        env.cleanSimulatorHandlers()

        APIMigrateVmMsg msg = new APIMigrateVmMsg()
        msg.hostUuid = host2.uuid
        msg.vmInstanceUuid = vm1.uuid
        def canceled = false
        def migrating = false

        env.simulator(KVMConstant.KVM_MIGRATE_VM_PATH) { HttpEntity<String> e ->
            migrating = true
            while (!canceled) {
                sleep(500)
            }
            return new KVMAgentCommands.MigrateVmResponse()
        }
        KVMAgentCommands.CancelCmd cmd = null
        env.simulator(AgentConstant.CANCEL_JOB) { HttpEntity<String> e ->
            cmd = json(e.body, KVMAgentCommands.CancelCmd.class)
            canceled = true
            def rsp = new KVMAgentCommands.CancelRsp()
            rsp.setError(LongJobConstants.NO_JOB_TO_CANCEL)
            return rsp
        }

        LongJobInventory jobInv = submitLongJob {
            jobName = msg.getClass().getSimpleName()
            jobData = gson.toJson(msg)
        } as LongJobInventory

        assert jobInv.jobName == msg.getClass().getSimpleName()
        assert jobInv.state == LongJobState.Running

        while (!migrating) {
            sleep(500)
        }
        expectError {
            cancelLongJob {
                uuid = jobInv.uuid
            }
        }

        retryInSecs() {
            LongJobVO job = dbFindByUuid(jobInv.getUuid(), LongJobVO.class)
            assert job.state.toString() == LongJobState.Succeeded.toString()
        }
        assert host2.uuid == dbf.findByUuid(vm1.uuid, VmInstanceVO.class).hostUuid
        assert canceled
        assert cmd.getInterval() == 1
        assert cmd.getTimes() == 3

        SQL.New(LongJobVO.class).eq(LongJobVO_.uuid, jobInv.getUuid()).set(LongJobVO_.state, org.zstack.header.longjob.LongJobState.Canceled).update();

        cancelLongJob {
            uuid = jobInv.uuid
        }

        SQL.New(LongJobVO.class).eq(LongJobVO_.uuid, jobInv.getUuid()).set(LongJobVO_.state, org.zstack.header.longjob.LongJobState.Succeeded).update();

        env.cleanSimulatorHandlers()
    }

    void testLiveMigrateVmLongJobCancelFail() {
        APIMigrateVmMsg msg = new APIMigrateVmMsg()
        msg.hostUuid = host1.uuid
        msg.vmInstanceUuid = vm1.uuid
        def canceled = false
        def migrating = false

        LongJobInventory jobInv
        env.simulator(KVMConstant.KVM_MIGRATE_VM_PATH) { HttpEntity<String> e ->
            migrating = true
            while (!canceled) {
                sleep(500)
            }
            assert Q.New(LongJobVO.class).eq(LongJobVO_.uuid, jobInv.uuid).select(LongJobVO_.state)
                    .findValue().toString() == LongJobState.Canceling.toString()
            return new KVMAgentCommands.MigrateVmResponse()
        }

        env.simulator(AgentConstant.CANCEL_JOB) {
            canceled = true
            def rsp = new KVMAgentCommands.CancelRsp()
            rsp.setError("on purpose")
            return rsp
        }

        jobInv = submitLongJob {
            jobName = msg.getClass().getSimpleName()
            jobData = gson.toJson(msg)
        } as LongJobInventory

        assert jobInv.jobName == msg.getClass().getSimpleName()
        assert jobInv.state == LongJobState.Running

        while (!migrating) {
            sleep(500)
        }

        expectError {
            cancelLongJob {
                uuid = jobInv.uuid
            }
        }

        retryInSecs() {
            LongJobVO job = dbFindByUuid(jobInv.getUuid(), LongJobVO.class)
            assert job.state.toString() == LongJobState.Succeeded.toString()
        }
        assert host1.uuid == dbf.findByUuid(vm1.uuid, VmInstanceVO.class).hostUuid
        assert canceled
    }

    void testLiveMigrateVmLongJobCancelBeforeMigrate() {
        env.cleanSimulatorHandlers()

        APIMigrateVmMsg msg = new APIMigrateVmMsg()
        msg.hostUuid = host2.uuid
        msg.vmInstanceUuid = vm1.uuid
        def canceled = false
        def migrating = false

        env.simulator(AgentConstant.CANCEL_JOB) {
            canceled = true
            def rsp = new KVMAgentCommands.CancelRsp()
            rsp.setError("on purpose")
            return rsp
        }

        env.simulator(KVMConstant.KVM_MIGRATE_VM_PATH) { HttpEntity<String> e ->
            migrating = true
            return new KVMAgentCommands.MigrateVmResponse()
        }

        env.message(AllocateHostMsg.class) { AllocateHostMsg amsg, CloudBus bus ->
            while (!canceled) {
                sleep(500)
            }

            long mem = Q.New(HostCapacityVO.class).eq(HostCapacityVO_.uuid, host2.uuid).select(HostCapacityVO_.availableMemory).findValue()
            long cpu = Q.New(HostCapacityVO.class).eq(HostCapacityVO_.uuid, host2.uuid).select(HostCapacityVO_.availableCpu).findValue()
            long afterAllocateMemInByte = mem - instance.memorySize
            long afterAllocateCpuCount = cpu - instance.cpuNum

            SQL.New(HostCapacityVO.class).eq(HostCapacityVO_.uuid, host2.uuid)
                    .set(HostCapacityVO_.availableMemory, afterAllocateMemInByte)
                    .set(HostCapacityVO_.availableCpu, afterAllocateCpuCount)
                    .update()

            AllocateHostReply r = new AllocateHostReply()
            r.setHost(org.zstack.header.host.HostInventory.valueOf(Q.New(HostVO.class).eq(HostVO_.uuid, host2.uuid).find() as HostVO))
            bus.reply(amsg, r)
        }

        LongJobInventory jobInv = submitLongJob {
            jobName = msg.getClass().getSimpleName()
            jobData = gson.toJson(msg)
        } as LongJobInventory

        canceled = false
        Thread thread = new Thread(new Runnable() {
            @Override
            void run() {
                expectError {
                    cancelLongJob {
                        uuid = jobInv.uuid
                    }
                }
                canceled = true
            }
        })

        thread.start()
        thread.join()

        assert canceled
        assert jobInv.jobName == msg.getClass().getSimpleName()
        assert jobInv.state == LongJobState.Running

        retryInSecs() {
            LongJobVO job = dbFindByUuid(jobInv.getUuid(), LongJobVO.class)
            assert job.state.toString() == LongJobState.Canceled.toString()
        }
        assert host1.uuid == dbf.findByUuid(vm1.uuid, VmInstanceVO.class).hostUuid
        assert !migrating
    }
}
