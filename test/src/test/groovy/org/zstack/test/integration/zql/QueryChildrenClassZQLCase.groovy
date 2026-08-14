package org.zstack.test.integration.zql

import org.zstack.sdk.CephPrimaryStorageInventory
import org.zstack.storage.ceph.CephConstants
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

class QueryChildrenClassZQLCase extends SubCase{
    EnvSpec env

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = makeEnv {
            zone {
                name = "zone"

                cephPrimaryStorage {
                    name = "ceph-pri"
                    description = "Test"
                    url = "ceph://pri"
                    fsid = "7ff218d9-f525-435f-8a40-3618d1772a64"
                    monUrls = ["root:password@localhost/?monPort=7777",
                               "root:password@127.0.0.3/?monPort=7777",
                               "root:password@127.0.0.4/?monPort=7777"]
                }
            }
        }
    }

    @Override
    void test() {
        env.create {
            def ps = queryPrimaryStorage {
                conditions=["type=${CephConstants.CEPH_PRIMARY_STORAGE_TYPE}", "availableCapacity>=0"]
            }
            assert ps.size() == 1
            assert ps[0] instanceof CephPrimaryStorageInventory
        }
    }

    @Override
    void clean() {
        env.delete()
    }
}
