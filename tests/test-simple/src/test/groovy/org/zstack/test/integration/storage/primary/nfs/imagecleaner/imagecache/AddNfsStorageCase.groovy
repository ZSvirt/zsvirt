package org.zstack.test.integration.storage.primary.nfs.imagecleaner.imagecache

import org.zstack.core.db.SQL
import org.zstack.header.storage.primary.PrimaryStorageVO
import org.zstack.header.storage.primary.PrimaryStorageVO_
import org.zstack.sdk.AddNfsPrimaryStorageAction
import org.zstack.sdk.UpdatePrimaryStorageAction
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.ZoneSpec
/**
 * Created by camile on 2017/4/
 */
class AddNfsStorageCase extends SubCase {
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
            testNfsUrlValidationAndNormalization()
            testValidateStorageNetwork()
        }
    }

    void testNfsUrlValidationAndNormalization() {
        String zoneUuid = (env.specByName("zone") as ZoneSpec).inventory.uuid

        [
                "192.168.1.196:/dev",
                "192.168.1.196:/dev/test",
                "192.168.1.196:/proc",
                "192.168.1.196:/proc/test",
                "192.168.1.196:/sys",
                "192.168.1.196:/sys/test",
                "192.168.1.196:relative/path",
                ":/nfs",
                "192.168.1.196"
        ].eachWithIndex { String url, int index ->
            AddNfsPrimaryStorageAction.Result res = addNfsPrimaryStorage(url, "invalid-nfs-${index}", zoneUuid)
            assert res.error != null: "URL should be rejected: ${url}"
        }

        AddNfsPrimaryStorageAction.Result systemResult = addNfsPrimaryStorage(
                "192.168.1.196:/system", "system-export", zoneUuid)
        assert systemResult.error == null
        assert systemResult.value.inventory.url == "192.168.1.196:/system"

        AddNfsPrimaryStorageAction.Result syslogResult = addNfsPrimaryStorage(
                "192.168.1.197:/syslog", "syslog-export", zoneUuid)
        assert syslogResult.error == null
        assert syslogResult.value.inventory.url == "192.168.1.197:/syslog"

        AddNfsPrimaryStorageAction.Result normalizedResult = addNfsPrimaryStorage(
                "192.168.1.198://exports//data/", "normalized-export", zoneUuid)
        assert normalizedResult.error == null
        assert normalizedResult.value.inventory.url == "192.168.1.198:/exports/data"

        SQL.New(PrimaryStorageVO.class)
                .eq(PrimaryStorageVO_.uuid, normalizedResult.value.inventory.uuid)
                .set(PrimaryStorageVO_.url, "192.168.1.198://exports//data/")
                .update()
        AddNfsPrimaryStorageAction.Result duplicateHistoricalUrlResult = addNfsPrimaryStorage(
                "192.168.1.198:/exports/data", "duplicate-historical-export", zoneUuid)
        assert duplicateHistoricalUrlResult.error != null

        UpdatePrimaryStorageAction update = new UpdatePrimaryStorageAction()
        update.uuid = normalizedResult.value.inventory.uuid
        update.url = "192.168.1.198:/exports/data"
        update.sessionId = adminSession()
        UpdatePrimaryStorageAction.Result updateResult = update.call()
        assert updateResult.error == null
        assert updateResult.value.inventory.url == "192.168.1.198:/exports/data"

        update.url = "192.168.1.198:relative/path"
        UpdatePrimaryStorageAction.Result invalidUpdateResult = update.call()
        assert invalidUpdateResult.error != null
    }

    private AddNfsPrimaryStorageAction.Result addNfsPrimaryStorage(String url, String name, String zoneUuid) {
        AddNfsPrimaryStorageAction action = new AddNfsPrimaryStorageAction()
        action.url = url
        action.name = name
        action.zoneUuid = zoneUuid
        action.sessionId = adminSession()
        return action.call()
    }

    void testValidateStorageNetwork() {
        String zoneUuid = env.inventoryByName("zone").uuid
        AddNfsPrimaryStorageAction addNfsPrimaryStorageAction = new AddNfsPrimaryStorageAction()
        addNfsPrimaryStorageAction.url = "192.168.1.197:/nfs"
        addNfsPrimaryStorageAction.name = "test2"
        addNfsPrimaryStorageAction.zoneUuid = zoneUuid
        addNfsPrimaryStorageAction.sessionId = adminSession()
        addNfsPrimaryStorageAction.systemTags = ["primaryStorage::gateway::cidr::192.168.2.1/24"]
        AddNfsPrimaryStorageAction.Result res = addNfsPrimaryStorageAction.call()
        assert res.error != null

        addNfsPrimaryStorageAction.systemTags = ["primaryStorage::gateway::cidr::192.168.1.1/24"]
        res = addNfsPrimaryStorageAction.call()
        assert res.error == null
    }
}
