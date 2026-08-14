package org.zstack.test.integration.zql

import org.zstack.sdk.SessionInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.test.integration.storage.Env
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

class NormalAccountZQLCase extends SubCase{
    EnvSpec env
    String batchQuery = """
def tmp = query("QueryZone")

tmp = zql("query vminstance")
put("vm", tmp)
"""

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.localStorageOneVmEnv()
    }

    @Override
    void test() {
        env.create {
//            testAdminQuery()
//            testNormalAccountQuery()
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    void testAdminQuery(){
        Map out = batchQuery { script = batchQuery }.result
        List vms = out["vm"].get(0)["inventories"]
        assert vms.size() == 2
    }

    void testNormalAccountQuery(){
        createAccount {
            name = "test"
            password = "password"
        }

        def normalSession = logInByAccount {
            accountName = "test"
            password = "password"
        } as SessionInventory

        Map out = batchQuery {
            script = batchQuery
            sessionId = normalSession.uuid
        }.result
        List vms = out["vm"].get(0)["inventories"]
        assert vms.size() == 0
    }
}
