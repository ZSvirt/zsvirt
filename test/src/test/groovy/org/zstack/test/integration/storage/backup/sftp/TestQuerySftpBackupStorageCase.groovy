package org.zstack.test.integration.storage.backup.sftp

import org.zstack.sdk.*
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.util.search.SDKQueryTestValidator
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

/**
 * Created by lining on 2017/3/27.
 */
// base on TestQuerySftpBackupStorage
class TestQuerySftpBackupStorageCase extends SubCase {

    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(NetworkServiceProviderTest.springSpec)
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
            testUserPolicyAllow()
        }
    }

    void testUserPolicyAllow(){

        SftpBackupStorageInventory sftp = env.inventoryByName("sftp")

        def checked = retryInSecs(2,1){
            try {
                sftp = querySftpBackupStorage {
                    conditions = ["uuid=${sftp.uuid}".toString()]
                }[0]
                SDKQueryTestValidator.validateEQ(new QuerySftpBackupStorageAction(),sftp)
                return true
            }catch (Exception e){
                return false
            }
        }
        assert checked

    }

}
