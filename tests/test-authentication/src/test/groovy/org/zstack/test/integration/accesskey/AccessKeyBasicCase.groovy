package org.zstack.test.integration.accesskey

import org.zstack.header.identity.AccountConstant
import org.zstack.sdk.AccessKeyInventory
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

class AccessKeyBasicCase extends SubCase {
    EnvSpec env

    @Override
    void setup() {
        useSpring(AccessKeyTest.springSpec)
    }

    @Override
    void environment() {
        env = makeEnv {}
    }

    @Override
    void test() {
        env.create {
            testCreateAk()
        }
    }

    void testCreateAk() {
        logger.info("Test 001: test create access-key")

        def inv001 = createAccessKey {
            delegate.accountUuid = AccountConstant.INITIAL_SYSTEM_ADMIN_UUID
        } as AccessKeyInventory

        assert inv001.AccessKeyID
        assert inv001.AccessKeySecret == null

        def invs001 = queryAccessKey {
            delegate.conditions = [
                "uuid=" + inv001.uuid
            ]
        } as List<AccessKeyInventory>
        assert invs001.size() == 1

        assert invs001[0].AccessKeyID
        assert invs001[0].AccessKeySecret
    }

    @Override
    void clean() {
        env.delete()
    }
}
