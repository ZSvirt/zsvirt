package org.zstack.test.integration.storage.primary.ceph

import org.springframework.http.HttpEntity
import org.zstack.core.db.Q
import org.zstack.kvm.KVMConstant
import org.zstack.storage.ceph.primary.CephPrimaryStorageBase
import org.zstack.storage.ceph.primary.CephPrimaryStorageMonVO
import org.zstack.storage.ceph.primary.CephPrimaryStorageMonVO_
import org.zstack.test.integration.storage.CephEnv
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

/**
 * Created by xing5 on 2017/3/17.
 */
class AttachIsoMonAddrDifferentCase extends SubCase {
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

    void testAttachIsoWithCephMonAddrNotEqualToCephManagementIp() {
        String psUuid = env.inventoryByName("ceph-pri").uuid

        String monAddr = "192.168.6.100"
        env.cleanSimulatorAndMessageHandlers()
        env.afterSimulator(CephPrimaryStorageBase.GET_FACTS) { CephPrimaryStorageBase.GetFactsRsp rsp, _ ->
            rsp.monAddr = monAddr
            return rsp
        }

        // reconnect ceph to get the new mon addr
        reconnectPrimaryStorage {
            uuid = psUuid
        }

        CephPrimaryStorageMonVO mon = Q.New(CephPrimaryStorageMonVO.class)
                .eq(CephPrimaryStorageMonVO_.primaryStorageUuid, psUuid)
                .find()

        assert mon.monAddr == monAddr

        String vmUuid = env.inventoryByName("test-vm").uuid
        String imageUuid = env.inventoryByName("test-iso").uuid

        Map cmd = null
        env.afterSimulator(KVMConstant.KVM_ATTACH_ISO_PATH) { rsp, HttpEntity<String> e ->
            cmd = json(e.body, LinkedHashMap.class)
            return rsp
        }

        attachIsoToVmInstance {
            vmInstanceUuid = vmUuid
            isoUuid = imageUuid
        }

        assert cmd != null
        assert monAddr == cmd.iso.monInfo[0].hostname
    }

    @Override
    void test() {
        env.create {
            testAttachIsoWithCephMonAddrNotEqualToCephManagementIp()
        }
    }
}
