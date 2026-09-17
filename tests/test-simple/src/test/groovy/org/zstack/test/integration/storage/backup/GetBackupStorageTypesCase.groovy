package org.zstack.test.integration.storage.backup

import org.zstack.test.integration.storage.Env
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

/**
 * Created by Qi Le on 2020/5/27
 */
class GetBackupStorageTypesCase extends SubCase {
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
        env = Env.localStorageOneVmEnv()
    }

    @Override
    void test() {
        env.create {
            testGetBackupStorageTypes()
        }
    }

    void testGetBackupStorageTypes() {
        def res = getBackupStorageTypes {}
        List types = res.types
        assert types != null
        assert !types.isEmpty()
    }
}
