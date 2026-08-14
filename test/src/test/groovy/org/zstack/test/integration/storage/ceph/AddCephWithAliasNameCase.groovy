package org.zstack.test.integration.storage.ceph

import org.springframework.http.HttpEntity
import org.zstack.core.Platform
import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.sdk.AddCephPrimaryStorageAction
import org.zstack.storage.ceph.CephSystemTags
import org.zstack.storage.ceph.backup.CephBackupStorageBase
import org.zstack.storage.ceph.backup.CephBackupStorageMonVO
import org.zstack.storage.ceph.backup.CephBackupStorageMonVO_
import org.zstack.storage.ceph.backup.CephBackupStorageVO
import org.zstack.storage.ceph.backup.CephBackupStorageVO_
import org.zstack.storage.ceph.primary.CephPrimaryStorageBase
import org.zstack.storage.ceph.primary.CephPrimaryStorageMonBase
import org.zstack.storage.ceph.primary.CephPrimaryStoragePoolVO
import org.zstack.storage.ceph.primary.CephPrimaryStoragePoolVO_
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

/**
 * Created by AlanJager on 2017/9/1.
 */
class AddCephWithAliasNameCase extends SubCase {
    EnvSpec env

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
        env = makeEnv {
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

    void testAddCephWithAliasPoolName() {
        String fsid = "7ff218d9-f525-435f-8a40-3618d1772a64"

        env.simulator(CephPrimaryStorageMonBase.ECHO_PATH) { HttpEntity<String> entity, EnvSpec spec ->
            return [:]
        }

        env.simulator(CephPrimaryStorageBase.GET_FACTS) { HttpEntity<String> e, EnvSpec spec ->
            def rsp = new CephPrimaryStorageBase.GetFactsRsp()
            return rsp
        }

        env.simulator(CephPrimaryStorageBase.INIT_PATH) { HttpEntity<String> e, EnvSpec spec ->
            def rsp = new CephPrimaryStorageBase.InitRsp()
            rsp.fsid = fsid
            rsp.availableCapacity = SizeUnit.GIGABYTE.toByte(1)
            rsp.totalCapacity = SizeUnit.GIGABYTE.toByte(1)
            return rsp
        }

        AddCephPrimaryStorageAction action = new AddCephPrimaryStorageAction()
        action.name = "ceph-primary-new"
        action.monUrls = ["root:password@localhost"]
        action.rootVolumePoolName = "pool-root"
        action.dataVolumePoolName = "pool-data"
        action.imageCachePoolName = "pool-image_cache"
        action.zoneUuid = env.inventoryByName("zone1").uuid
        action.sessionId = adminSession()
        AddCephPrimaryStorageAction.Result ret = action.call()

        assert ret.error == null
        String aliasDataPool = "data_volume_pool_alias"
        String poolName = CephSystemTags.DEFAULT_CEPH_PRIMARY_STORAGE_DATA_VOLUME_POOL.getTokenByResourceUuid(ret.value.inventory.uuid, CephSystemTags.DEFAULT_CEPH_PRIMARY_STORAGE_DATA_VOLUME_POOL_TOKEN)
        String poolUuid = Q.New(CephPrimaryStoragePoolVO.class)
                .select(CephPrimaryStoragePoolVO_.uuid)
                .eq(CephPrimaryStoragePoolVO_.poolName, poolName)
                .eq(CephPrimaryStoragePoolVO_.primaryStorageUuid, ret.value.inventory.uuid)
                .findValue()

        updateCephPrimaryStoragePool {
            uuid = poolUuid
            aliasName = aliasDataPool
        }

        String aliasName = Q.New(CephPrimaryStoragePoolVO.class)
                .select(CephPrimaryStoragePoolVO_.aliasName)
                .eq(CephPrimaryStoragePoolVO_.uuid, poolUuid)
                .eq(CephPrimaryStoragePoolVO_.primaryStorageUuid, ret.value.inventory.uuid)
                .findValue()

        assert aliasName == aliasDataPool

        SQL.New(CephPrimaryStoragePoolVO.class).delete()
    }

    void testFailedToAddCephBsAndCheckMonVo() {
        env.cleanSimulatorHandlers()
        String cephPsUuid = Platform.getUuid()

        env.simulator(CephBackupStorageBase.GET_FACTS) { HttpEntity<String> e, EnvSpec spec ->
            def cmd = JSONObjectUtil.toObject(e.body, CephPrimaryStorageBase.GetFactsCmd.class)
            def rsp = new CephPrimaryStorageBase.GetFactsRsp()
            CephBackupStorageMonVO mon = dbFindByUuid(cmd.monUuid, CephBackupStorageMonVO.class)
            if (mon != null && mon.getHostname() == "127.0.0.3") {
                rsp.setSuccess(false)
                rsp.setError("failed to GET_FACTS on purpose")
            }
            if (mon != null && mon.getHostname() == "127.0.0.1") {
                sleep(1000)
            }
            return rsp
        }

        expectError {
            addCephBackupStorage {
                resourceUuid = cephPsUuid
                name = "ceph-primary"
                url = "/ceph"
                monUrls = ["root:password@127.0.0.1", "root:password@127.0.0.2", "root:password@127.0.0.3"]
                poolName = "pool-name"
            }
        }
        assert !Q.New(CephBackupStorageMonVO.class).eq(CephBackupStorageMonVO_.backupStorageUuid, cephPsUuid).isExists()
        env.cleanSimulatorHandlers()
    }

    @Override
    void test() {
        env.create {
            testAddCephWithAliasPoolName()
            testFailedToAddCephBsAndCheckMonVo()
        }
    }
}
