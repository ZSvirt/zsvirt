package org.zstack.test.integration.image

import org.zstack.testlib.EnvSpec
import org.zstack.testlib.Test
import org.zstack.utils.data.SizeUnit

/**
 * Created by xing5 on 2017/3/5.
 */
class Env {
    static EnvSpec oneSftpEnv = Test.makeEnv {
        zone {
            name = "zone"
        }

        sftpBackupStorage {
            name = "sftp"
            url = "/sftp"

            image {
                name = "image"
                url = "http://zstack.org/download/image.qcow2"
            }
        }
    }

    static EnvSpec oneCephBackupStorageEnv() {
        return Test.makeEnv {
            zone {
                name = "zone"
                attachBackupStorage("ceph-bk")
            }

            cephBackupStorage {
                name = "ceph-bk"
                description = "Test"
                totalCapacity = SizeUnit.GIGABYTE.toByte(100)
                availableCapacity = SizeUnit.GIGABYTE.toByte(100)
                url = "/bk"
                fsid = "7ff218d9-f525-435f-8a40-3618d1772a64"
                monUrls = ["root:password@localhost/?monPort=7777"]
            }
        }
    }
}
