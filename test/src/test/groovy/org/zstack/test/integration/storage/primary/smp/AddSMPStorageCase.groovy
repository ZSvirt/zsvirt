package org.zstack.test.integration.storage.primary.smp

import org.zstack.sdk.AddSharedMountPointPrimaryStorageAction
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.ZoneSpec

/**
 * Created by camile on 2017/4/
 */
class AddSMPStorageCase extends SubCase {
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
        env = env {
            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                username = "root"
                password = "password"
                hostname = "localhost"
            }

            zone {
                name = "zone"
                description = "test"

                attachBackupStorage("sftp")
            }
        }
    }

    @Override
    void test() {
        env.create {
            addErrorPathLSailure()
        }
    }

    void addErrorPathLSailure() {
        String zoneUuid = (env.specByName("zone") as ZoneSpec).inventory.uuid
        AddSharedMountPointPrimaryStorageAction addLocalPrimaryStorageAction = new AddSharedMountPointPrimaryStorageAction()
        addLocalPrimaryStorageAction.url = "/dev/test"
        addLocalPrimaryStorageAction.name = "test2"
        addLocalPrimaryStorageAction.zoneUuid = zoneUuid
        addLocalPrimaryStorageAction.sessionId = adminSession()
        AddSharedMountPointPrimaryStorageAction.Result res = addLocalPrimaryStorageAction.call()
        assert res.error != null
        addLocalPrimaryStorageAction.url = "/proc/test"
        res = addLocalPrimaryStorageAction.call()
        assert  res.error != null
        addLocalPrimaryStorageAction.url = "/sys/test"
        res = addLocalPrimaryStorageAction.call()
        assert res.error != null
    }
}
