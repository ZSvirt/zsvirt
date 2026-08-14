package org.zstack.test.integration.storage.primary.local

import org.springframework.http.HttpEntity
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.sdk.ClusterInventory
import org.zstack.sdk.GetLocalStorageHostDiskCapacityAction
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.test.integration.kvm.Env
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

class LocalStorageCalculateCapacityCase extends SubCase {
    EnvSpec env
    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        spring {
            sftpBackupStorage()
            localStorage()
            virtualRouter()
            securityGroup()
            kvm()
        }
    }

    @Override
    void environment() {
        env = Env.oneVmBasicEnv()
    }

    @Override
    void test() {
        env.create {
            testCalculateCapacityWhenAddLocalStorage()
        }
    }

    void testCalculateCapacityWhenAddLocalStorage(){
        ClusterInventory cluster = env.inventoryByName("cluster")
        PrimaryStorageInventory ps = env.inventoryByName("local")

        boolean temp = false

        env.afterSimulator(KVMConstant.KVM_CONNECT_PATH) { rsp, HttpEntity<String> entity ->
            rsp.success = true

            def cmd = json(entity.getBody(),KVMAgentCommands.ConnectCmd.class)
            GetLocalStorageHostDiskCapacityAction action = new GetLocalStorageHostDiskCapacityAction()
            action.hostUuid = cmd.hostUuid
            action.primaryStorageUuid = ps.uuid
            action.sessionId = adminSession()
            GetLocalStorageHostDiskCapacityAction.Result res = action.call()

            assert res.error == null
            temp = true

            return rsp
        }

        addKVMHost {
            username = "test"
            password = "password"
            name = "adding-host"
            managementIp = "127.0.0.2"
            clusterUuid  = cluster.uuid
        }

        assert temp
    }
}
