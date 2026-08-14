package org.zstack.test.integration.kvm.host

import org.springframework.http.HttpEntity
import org.zstack.core.Platform
import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.db.Q
import org.zstack.header.errorcode.SysErrors
import org.zstack.header.host.AddHostReply
import org.zstack.header.host.HostConstant
import org.zstack.header.host.HostStatus
import org.zstack.header.host.HostVO
import org.zstack.kvm.APIAddKVMHostMsg
import org.zstack.kvm.AddKVMHostMsg
import org.zstack.kvm.KVMHostInventory
import org.zstack.sdk.AddKVMHostAction
import org.zstack.sdk.ClusterInventory
import org.zstack.sdk.GetHypervisorTypesResult
import org.zstack.sdk.LongJobInventory
import org.zstack.sdk.SystemTagInventory
import org.zstack.storage.primary.local.LocalStorageKvmBackend
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil

import static org.zstack.kvm.KVMConstant.*
import static org.zstack.kvm.KVMAgentCommands.*

/**
 * Created by mingjian.deng on 2019/1/3.*/
class AddHostCase extends SubCase {
    EnvSpec env
    ClusterInventory cluster
    CloudBus bus

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = HostEnv.noHostBasicEnv()
    }

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void test() {
        env.create {
            prepare()
            testCheckHostVersionFailure()
            testCheckHostManagementFailure()
            testInnerAddHostMsg()
            testGetHypervisorTypes()
            testAddHostFailureRollback()
            testAddHostViaLongJob()
            testLongJobAddHostFailure()
        }
    }

    void prepare() {
        cluster = env.inventoryByName("cluster") as ClusterInventory
        bus = bean(CloudBus.class)
    }

    void testLongJobAddHostFailure() {
        env.afterSimulator(KVM_HOST_FACT_PATH) { HostFactResponse rsp -> rsp }

        AddKVMHostMsg akmsg = new AddKVMHostMsg(
                accountUuid: loginAsAdmin().accountUuid,
                name: "kvm",
                managementIp: "###",
                clusterUuid: cluster.uuid,
                password: "password",
                username: "root"
        )

        LongJobInventory jobInventory
        expect(AssertionError.class) {
            jobInventory = submitLongJob {
                name = "addHostLongJob"
                jobName = APIAddKVMHostMsg.class.simpleName
                jobData = JSONObjectUtil.toJsonString(akmsg)
            } as LongJobInventory
        }

        //127.0.0.5 is added in testAddHostViaLongJob
        akmsg = new AddKVMHostMsg(
                accountUuid: loginAsAdmin().accountUuid,
                name: "kvm",
                managementIp: "127.0.0.5",
                clusterUuid: cluster.uuid,
                password: "password",
                username: "root"
        )

        expect(AssertionError.class) {
            jobInventory = submitLongJob {
                name = "addHostLongJob"
                jobName = APIAddKVMHostMsg.class.simpleName
                jobData = JSONObjectUtil.toJsonString(akmsg)
            } as LongJobInventory
        }
    }

    void testAddHostViaLongJob() {
        env.afterSimulator(KVM_HOST_FACT_PATH) { HostFactResponse rsp -> rsp }

        AddKVMHostMsg akmsg = new AddKVMHostMsg(
                accountUuid: loginAsAdmin().accountUuid,
                name: "kvm",
                managementIp: "127.0.0.5",
                clusterUuid: cluster.uuid,
                password: "password",
                username: "root"
        )

        LongJobInventory jobInventory = submitLongJob {
            name = "addHostLongJob"
            jobName = APIAddKVMHostMsg.class.simpleName
            jobData = JSONObjectUtil.toJsonString(akmsg)
        } as LongJobInventory

        String jobUuid = jobInventory.uuid

        retryInSecs {
            List list = queryLongJob {
                conditions = ["uuid=$jobUuid".toString()]
            } as List
            assert !list.isEmpty()
            jobInventory = list.first() as LongJobInventory
            assert jobInventory.state.toString() == "Succeeded"
        }
    }

    void testAddHostFailureRollback() {
        def initCalled = false
        def hangOnHostContinueConnectFlow = true

        env.afterSimulator(LocalStorageKvmBackend.INIT_PATH) { rsp, HttpEntity<String> e ->
            initCalled = true

            while (hangOnHostContinueConnectFlow) {
                sleep(1000)
            }

            return rsp
        }

        def res = null
        def hostUuid = Platform.uuid
        def addHostThread = Thread.start {
            def action = new AddKVMHostAction()
            action.sessionId = adminSession()
            action.resourceUuid = hostUuid
            action.clusterUuid = cluster.uuid
            action.managementIp = "127.0.0.3"
            action.name = "kvm"
            action.username = "root"
            action.password = "password"
            res = action.call()
        }

        def deleteHostThread = Thread.start {
            retryInSecs {
                assert initCalled
            }

            deleteHost {
                uuid = hostUuid
            }

            hangOnHostContinueConnectFlow = false
        }

        addHostThread.join()
        deleteHostThread.join()

        retryInSecs {
            assert res.error != null
        }
    }

    void testCheckHostVersionFailure() {
        env.afterSimulator(KVM_HOST_FACT_PATH) { HostFactResponse rsp ->
            rsp.osVersion = null
            return rsp
        }

        def action = new AddKVMHostAction()
        action.sessionId = adminSession()
        action.resourceUuid = Platform.uuid
        action.clusterUuid = cluster.uuid
        action.managementIp = "127.0.0.2"
        action.name = "kvm"
        action.username = "root"
        action.password = "password"
        def res = action.call()

        assert res.error != null
        assert Q.New(HostVO.class).count() == 0
    }

    void testCheckHostManagementFailure() {
        env.afterSimulator(KVM_HOST_FACT_PATH) { HostFactResponse rsp -> rsp }

        def action = new AddKVMHostAction()
        action.sessionId = adminSession()
        action.resourceUuid = Platform.uuid
        action.clusterUuid = cluster.uuid
        action.managementIp = "###"
        action.name = "kvm"
        action.username = "root"
        action.password = "password"
        def res = action.call()

        assert res.error != null
        assert res.error.code == SysErrors.INVALID_ARGUMENT_ERROR.toString()
        assert Q.New(HostVO.class).count() == 0
    }

    void testInnerAddHostMsg() {
        String distribution = null
        String iscsiInitiatorName = null
        String nqn = null
        env.afterSimulator(KVM_HOST_FACT_PATH) { HostFactResponse rsp ->
            distribution = rsp.osDistribution
            iscsiInitiatorName = rsp.iscsiInitiatorName
            nqn = rsp.nqn
            return rsp
        }

        AddKVMHostMsg amsg = new AddKVMHostMsg()
        amsg.accountUuid = loginAsAdmin().accountUuid
        amsg.name = "kvm"
        amsg.managementIp = "127.0.0.2"
        amsg.resourceUuid = Platform.uuid
        amsg.clusterUuid = cluster.uuid
        amsg.setPassword("password")
        amsg.setUsername("root")

        bus.makeLocalServiceId(amsg, HostConstant.SERVICE_ID)
        AddHostReply reply = (AddHostReply) bus.call(amsg)
        assert reply.inventory.status == HostStatus.Connected.toString()

        // For compatibility.
        // OS system tag is deprecated, check os version with KVMHostVO soon.
        def tags = querySystemTag {
            delegate.conditions = [
                    "resourceUuid=${reply.inventory.uuid}",
                    "tag=os::distribution::${distribution}"
            ]
        } as List<SystemTagInventory>
        assert tags.size() == 1

        assert (reply.inventory as KVMHostInventory).osDistribution == distribution
        assert (reply.inventory as KVMHostInventory).iscsiInitiatorName == iscsiInitiatorName
        assert (reply.inventory as KVMHostInventory).nqn == nqn
    }

    void testGetHypervisorTypes() {
        GetHypervisorTypesResult result = getHypervisorTypes {
            sessionId = adminSession()
        }
        assert !result.getHypervisorTypes().isEmpty()
    }
}
