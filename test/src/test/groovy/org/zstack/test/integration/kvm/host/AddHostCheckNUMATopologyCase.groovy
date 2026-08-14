package org.zstack.test.integration.kvm.host

import com.google.common.collect.ImmutableMap
import org.springframework.http.HttpEntity
import org.zstack.core.Platform
import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.core.db.SimpleQuery
import org.zstack.header.errorcode.SysErrors
import org.zstack.header.host.*
import org.zstack.kvm.APIAddKVMHostMsg
import org.zstack.kvm.AddKVMHostMsg
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.sdk.AddKVMHostAction
import org.zstack.sdk.ClusterInventory
import org.zstack.sdk.GetHypervisorTypesResult
import org.zstack.sdk.LongJobInventory
import org.zstack.storage.primary.local.LocalStorageKvmBackend
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil

import static org.zstack.kvm.KVMConstant.KVM_HOST_FACT_PATH


class AddHostCheckNUMATopologyCase extends SubCase {
    EnvSpec env
    ClusterInventory cluster
    CloudBus bus
    DatabaseFacade dbf

    boolean getHostNUMACmdCall = false

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
        dbf = bean(DatabaseFacade.class)
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
        setHostNUMATopologyHook()
    }

    void setHostNUMATopologyHook() {
        env.simulator(KVMConstant.KVM_HOST_NUMA_PATH) { HttpEntity<String> e, EnvSpec env ->
            def cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.GetHostNUMATopologyCmd.class)
            assert !cmd.hostUuid.isEmpty()
            getHostNUMACmdCall = true

            def rsp = new  KVMAgentCommands.GetHostNUMATopologyResponse()
            Map<String, Object> node0 = ImmutableMap.of("distance", Arrays.asList("10", "21"),
                    "cpus", Arrays.asList("0","1","2","3","4","5","6","7"),
                    "free", 3889268,
                    "size", 38892686
            )
            Map<String, Object> node1 = ImmutableMap.of("distance", Arrays.asList("21", "10"),
                    "cpus", Arrays.asList("8","9","10","11","12","13","14","15"),
                    "free", 2889268,
                    "size", 48892686
            )
            Map<String, Map<String, Object>> topology = ImmutableMap.of("0", node0, "1", node1)
            rsp.setTopology(topology)
            return rsp
        }
    }

    void testLongJobAddHostFailure() {
        env.afterSimulator(KVM_HOST_FACT_PATH) { KVMAgentCommands.HostFactResponse rsp -> rsp }

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
        env.afterSimulator(KVM_HOST_FACT_PATH) { KVMAgentCommands.HostFactResponse rsp -> rsp }

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
        env.afterSimulator(KVM_HOST_FACT_PATH) { KVMAgentCommands.HostFactResponse rsp ->
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

        SQL.New("delete from HostNumaNodeVO where hostUuid = :uuid").param("uuid", action.resourceUuid).execute()
        SQL.New("delete from HostAllocatedCpuVO where hostUuid = :uuid").param("uuid", action.resourceUuid).execute()
    }

    void testCheckHostManagementFailure() {
        env.afterSimulator(KVM_HOST_FACT_PATH) { KVMAgentCommands.HostFactResponse rsp -> rsp }

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
        env.afterSimulator(KVM_HOST_FACT_PATH) { KVMAgentCommands.HostFactResponse rsp -> rsp }

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

        assert getHostNUMACmdCall

        SimpleQuery<HostNumaNodeVO> hvoQuery = dbf.createQuery(HostNumaNodeVO.class);
        hvoQuery.add(HostNumaNodeVO_.hostUuid, SimpleQuery.Op.EQ, reply.inventory.uuid);
        List<HostNumaNodeVO> nodes = hvoQuery.list();

        assert !nodes.isEmpty()

        for (HostNumaNodeVO node: nodes) {
            if (node.nodeID == 0) {
                assert node.nodeCPUs.equals("0,1,2,3,4,5,6,7")
                assert node.nodeDistance.equals("10,21")
                assert node.nodeMemSize == 38892686
            }
            if (node.nodeID == 1) {
                assert node.nodeCPUs.equals("8,9,10,11,12,13,14,15")
                assert node.nodeDistance.equals("21,10")
                assert node.nodeMemSize == 48892686
            }
        }
    }

    void testGetHypervisorTypes() {
        GetHypervisorTypesResult result = getHypervisorTypes {
            sessionId = adminSession()
        }
        assert !result.getHypervisorTypes().isEmpty()
    }
}

