package org.zstack.test.integration.storage.primary.ceph

import org.springframework.http.HttpEntity
import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.Q
import org.zstack.header.storage.primary.PrimaryStorageVO
import org.zstack.kvm.KVMSystemTags
import org.zstack.sdk.AddCephPrimaryStorageAction
import org.zstack.storage.ceph.CephSystemTags
import org.zstack.storage.ceph.primary.CephPrimaryStorageBase
import org.zstack.storage.ceph.primary.CephPrimaryStorageMonBase
import org.zstack.storage.ceph.primary.CephPrimaryStorageMonVO
import org.zstack.storage.ceph.primary.CephPrimaryStoragePoolVO
import org.zstack.storage.ceph.primary.CephPrimaryStoragePoolVO_
import org.zstack.storage.ceph.primary.CephPrimaryStorageVO
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.HttpError
import org.zstack.testlib.SubCase
import org.zstack.testlib.Test
import org.zstack.utils.Utils
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil
import org.zstack.utils.logging.CLogger

class AddCephPrimaryStorageCase extends SubCase {
    private final static CLogger logger = Utils.getLogger(AddCephPrimaryStorageCase.class)

    def DOC = """
use:
1. add ceph primary storage. cephPSVO and monVOs would be created.
2. hook internal message[ConnectPrimaryStorageMsg], save and reply with failure. cephPSVO would be cleaned.
3. resend saved message, trigger NPE
"""


    EnvSpec env
    DatabaseFacade dbf
    CloudBus bus


    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
    }

    @Override
    void environment() {
        env = Test.makeEnv {
            zone {
                name = "zone1"
                cluster {
                    name = "cluster1"
                    hypervisorType = "KVM"

                    kvm {
                        name = "host1"
                        managementIp = "localhost"
                        username = "root"
                        password = "password"
                        usedMem = 1000
                        totalCpu = 10
                    }

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
            }
        }
    }

    void testAfterAddCephPrimaryStorageTimeoutStillConnect() {
        env.simulator(CephPrimaryStorageMonBase.ECHO_PATH) { HttpEntity<String> entity, EnvSpec spec ->
            throw new HttpError(404, "on purpose")
        }

        AddCephPrimaryStorageAction action = new AddCephPrimaryStorageAction()
        action.name = "ceph-primary-new"
        action.monUrls = ["root:password@localhost"]
        action.rootVolumePoolName = "rootPool"
        action.dataVolumePoolName = "dataPool"
        action.imageCachePoolName = "cachePool"
        action.zoneUuid = env.inventoryByName("zone1").uuid
        action.sessionId = adminSession()
        action.call()

        // wait echo finished
        retryInSecs{
            assert Q.New(CephPrimaryStorageMonVO.class).count() == 0l: "after failed to add cephPS, all monVO should be removed, but some left"
        }
    }

    void testAddCephPrimaryStorageGetFactsFailed() {
        env.simulator(CephPrimaryStorageMonBase.ECHO_PATH) { HttpEntity<String> e, EnvSpec spec ->
            return [:]
        }

        env.simulator(CephPrimaryStorageBase.INIT_PATH) { HttpEntity<String> e, EnvSpec spec ->
            def fsid = "7ff218d9-f525-435f-8a40-3618d1772a64"
            def rsp = new CephPrimaryStorageBase.InitRsp()
            rsp.fsid = fsid
            rsp.availableCapacity = SizeUnit.GIGABYTE.toByte(1)
            rsp.totalCapacity = SizeUnit.GIGABYTE.toByte(1)
            return rsp
        }

        // first try: simulate that the last mon fails to get facts
        env.simulator(CephPrimaryStorageBase.GET_FACTS) { HttpEntity<String> e, EnvSpec spec ->
            def cmd = JSONObjectUtil.toObject(e.body, CephPrimaryStorageBase.GetFactsCmd.class)
            def rsp = new CephPrimaryStorageBase.GetFactsRsp()
            CephPrimaryStorageMonVO mon = dbFindByUuid(cmd.monUuid, CephPrimaryStorageMonVO.class)
            if (mon != null && mon.getHostname() == "127.0.0.3") {
                rsp.setSuccess(false)
                rsp.setError("failed to GET_FACTS on purpose")
            }
            return rsp
        }

        AddCephPrimaryStorageAction action = new AddCephPrimaryStorageAction()
        action.name = "add-ceph-ps"
        action.monUrls = ["root:password@127.0.0.1", "root:password@127.0.0.2", "root:password@127.0.0.3"]
        action.rootVolumePoolName = "rootPool"
        action.dataVolumePoolName = "dataPool"
        action.imageCachePoolName = "cachePool"
        action.zoneUuid = env.inventoryByName("zone1").uuid
        action.sessionId = adminSession()
        action.call()

        assert Q.New(PrimaryStorageVO.class).count() == 0l
        assert Q.New(CephPrimaryStorageVO.class).count() == 0l
        assert Q.New(CephPrimaryStorageMonVO.class).count() == 0l
        assert Q.New(CephPrimaryStoragePoolVO.class).count() == 0l

        // second try: simulate that all mons succeeded to get facts
        env.simulator(CephPrimaryStorageBase.GET_FACTS)  { HttpEntity<String> e, EnvSpec spec ->
            return new CephPrimaryStorageBase.GetFactsRsp()
        }

        action = new AddCephPrimaryStorageAction()
        action.name = "add-ceph-ps"
        action.monUrls = ["root:password@127.0.0.1", "root:password@127.0.0.2", "root:password@127.0.0.3"]
        action.rootVolumePoolName = "rootPool"
        action.dataVolumePoolName = "dataPool"
        action.imageCachePoolName = "cachePool"
        action.zoneUuid = env.inventoryByName("zone1").uuid
        action.sessionId = adminSession()
        action.call()

        assert Q.New(PrimaryStorageVO.class).count() == 1l
        assert Q.New(CephPrimaryStorageVO.class).count() == 1l
        assert Q.New(CephPrimaryStoragePoolVO.class).count() == 3l
        assert Q.New(CephPrimaryStorageMonVO.class).count() == 3l
    }

    void testCreateInitializationPoolWithCustomName() {
        env.simulator(CephPrimaryStorageBase.GET_FACTS) { HttpEntity<String> e, EnvSpec spec ->
            def rsp = new CephPrimaryStorageBase.GetFactsRsp()
            rsp.fsid = "78f218d9-f525-435f-8a40-3618d1772a65"
            rsp.monAddr = "127.0.0.4"
            return rsp
        }

        env.simulator(CephPrimaryStorageBase.INIT_PATH) { HttpEntity<String> e, EnvSpec spec ->
            def cmd = JSONObjectUtil.toObject(e.body, CephPrimaryStorageBase.InitCmd.class)
            assert cmd.pools.stream().filter {v -> v.name == "testname" }.count() == 2

            def rsp = new CephPrimaryStorageBase.InitRsp()
            rsp.fsid = "78f218d9-f525-435f-8a40-3618d1772a65"
            rsp.availableCapacity = SizeUnit.GIGABYTE.toByte(1)
            rsp.totalCapacity = SizeUnit.GIGABYTE.toByte(1)
            return rsp
        }

        addCephPrimaryStorage {
            name = "test"
            monUrls = ["root:password@127.0.0.4"]
            zoneUuid = env.inventoryByName("zone1").uuid
            systemTags = ["ceph::customInitializationPoolName::testname"]
        }

        assert Q.New(CephPrimaryStoragePoolVO.class).eq(CephPrimaryStoragePoolVO_.poolName, "testname").count() == 2
        assert querySystemTag {conditions = ["tag=ceph::customInitializationPoolName::testname"]}.size() == 0
    }

    @Override
    void test() {
        dbf = bean(DatabaseFacade.class)
        bus = bean(CloudBus.class)

        env.create {
            testAfterAddCephPrimaryStorageTimeoutStillConnect()
            testAddCephPrimaryStorageGetFactsFailed()
            testCreateInitializationPoolWithCustomName()
        }
    }
}
