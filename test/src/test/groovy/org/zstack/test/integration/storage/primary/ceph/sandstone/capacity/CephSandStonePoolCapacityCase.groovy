package org.zstack.test.integration.storage.primary.ceph.sandstone.capacity

import org.springframework.http.HttpEntity
import org.zstack.core.Platform
import org.zstack.sdk.*
import org.zstack.storage.ceph.CephConstants
import org.zstack.storage.ceph.CephPoolCapacity
import org.zstack.storage.ceph.primary.CephPrimaryStorageBase
import org.zstack.test.integration.storage.CephEnv
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.CephPrimaryStorageSpec
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

/**
 * Created by lining on 2021/01/23.
 */
class CephSandStonePoolCapacityCase extends SubCase {
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
        env = CephEnv.CephStorageOneVmEnv()
    }

    @Override
    void test() {
        env.create {
            testReconnectPrimaryStorage()
        }
    }

    void testReconnectPrimaryStorage() {
        PrimaryStorageInventory ps = env.inventoryByName("ceph-pri")
        CephBackupStorageInventory bs = env.inventoryByName("ceph-bk")

        CephPrimaryStoragePoolInventory dataPool = queryCephPrimaryStoragePool {
            conditions = ["type=Data"]
        }[0]

        CephPrimaryStoragePoolInventory rootPool = queryCephPrimaryStoragePool {
            conditions = ["type=Root"]
        }[0]
        CephPrimaryStoragePoolInventory cachePool = queryCephPrimaryStoragePool {
            conditions = ["type=ImageCache"]
        }[0]

        long addSize = SizeUnit.GIGABYTE.toByte(100)

        Map<String, CephPoolCapacity.OsdCapacity> osdMap = new HashMap<>()
        osdMap.put("osd.1", new CephPoolCapacity.OsdCapacity(SizeUnit.GIGABYTE.toByte(90), SizeUnit.GIGABYTE.toByte(10), SizeUnit.GIGABYTE.toByte(100)))

        Map<String, CephPoolCapacity.OsdCapacity> osdMap2 = new HashMap<>()
        osdMap2.put("osd.1", new CephPoolCapacity.OsdCapacity(SizeUnit.GIGABYTE.toByte(200), SizeUnit.GIGABYTE.toByte(100), SizeUnit.GIGABYTE.toByte(300)))

        Map<String, CephPoolCapacity.OsdCapacity> osdMap3 = new HashMap<>()
        osdMap3.put("osd.2", new CephPoolCapacity.OsdCapacity(SizeUnit.GIGABYTE.toByte(100), SizeUnit.GIGABYTE.toByte(100), SizeUnit.GIGABYTE.toByte(200)))

        env.simulator(CephPrimaryStorageBase.INIT_PATH) { HttpEntity<String> e, EnvSpec spec ->
            def cmd = JSONObjectUtil.toObject(e.body, CephPrimaryStorageBase.InitCmd.class)
            CephPrimaryStorageSpec cspec = spec.specByUuid(cmd.uuid)

            def rsp = new CephPrimaryStorageBase.InitRsp()
            rsp.fsid = cspec.fsid
            rsp.userKey = Platform.uuid
            rsp.totalCapacity = 1000
            rsp.availableCapacity = 999
            rsp.poolCapacities = [
                    new CephPoolCapacity(
                            name : rootPool.poolName,
                            usedCapacity: rootPool.usedCapacity,
                            availableCapacity : rootPool.availableCapacity,
                            totalCapacity: rootPool.totalCapacity,
                            relatedOsds: "osd.1",
                            diskUtilization: 0.5,
                            relatedOsdCapacity: osdMap
                    ),
                    new CephPoolCapacity(
                            name : dataPool.poolName,
                            usedCapacity: dataPool.usedCapacity,
                            availableCapacity : dataPool.availableCapacity,
                            totalCapacity: dataPool.totalCapacity,
                            relatedOsds: "osd.1",
                            diskUtilization: 0.33,
                            relatedOsdCapacity: osdMap2
                    ),
                    new CephPoolCapacity(
                            name : cachePool.poolName,
                            usedCapacity: cachePool.usedCapacity,
                            availableCapacity : cachePool.availableCapacity,
                            totalCapacity: cachePool.totalCapacity,
                            diskUtilization: 0.33,
                            relatedOsds: "osd.2",
                            relatedOsdCapacity: osdMap3
                    ),
                    new CephPoolCapacity(
                            name : bs.poolName,
                            usedCapacity:  bs.getPoolUsedCapacity(),
                            availableCapacity : bs.availableCapacity + addSize,
                            totalCapacity: bs.totalCapacity + addSize,
                            relatedOsds: "osd.2"
                    ),
                    new CephPoolCapacity(
                            name : "other-pool",
                            availableCapacity : 10,
                            usedCapacity: 10,
                            totalCapacity: 20,
                            relatedOsds: "osd.3"
                    )
            ]
            rsp.type = CephConstants.CEPH_MANUFACTURER_SANDSTONE
            return rsp
        }

        reconnectPrimaryStorage {
            uuid = ps.uuid
        }

        GetPrimaryStorageCapacityResult beforeCap = getPrimaryStorageCapacity {
            primaryStorageUuids = [ps.uuid]
        }

        // 200*0.33 + 100*0.33
        assert beforeCap.totalCapacity == SizeUnit.GIGABYTE.toByte(165)
        assert beforeCap.totalPhysicalCapacity == SizeUnit.GIGABYTE.toByte(165)
        assert beforeCap.availablePhysicalCapacity == SizeUnit.GIGABYTE.toByte(99)

        osdMap2.put("osd.1", new CephPoolCapacity.OsdCapacity(SizeUnit.GIGABYTE.toByte(200) + addSize, SizeUnit.GIGABYTE.toByte(100), SizeUnit.GIGABYTE.toByte(300) + addSize))

        reconnectPrimaryStorage {
            uuid = ps.uuid
        }

        GetPrimaryStorageCapacityResult afterCap = getPrimaryStorageCapacity {
            primaryStorageUuids = [ps.uuid]
        }

        assert afterCap.availablePhysicalCapacity - beforeCap.availablePhysicalCapacity == addSize * 0.33
        assert afterCap.totalCapacity - beforeCap.totalCapacity == addSize * 0.33
        assert afterCap.totalPhysicalCapacity - beforeCap.totalPhysicalCapacity == addSize * 0.33
        retryInSecs {
            afterCap = getPrimaryStorageCapacity {
                primaryStorageUuids = [ps.uuid]
            }
            assert addSize * 0.33 == afterCap.availableCapacity - beforeCap.availableCapacity
        }

        BackupStorageInventory afterBs = queryBackupStorage {
            conditions = ["uuid=${bs.uuid}"]
        }[0]
        assert afterBs.availableCapacity == bs.availableCapacity + addSize
        assert afterBs.totalCapacity == bs.totalCapacity + addSize
    }
}
