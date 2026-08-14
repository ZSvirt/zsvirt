package org.zstack.test.integration.rest

import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.rest.AsyncRestVO
import org.zstack.rest.AsyncRestVO_
import org.zstack.sdk.KVMHostInventory
import org.zstack.test.integration.ZStackTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

/**
 * Created by kayo on 2017/9/7.
 */
class RestResultAPINoSeeFilterCase extends SubCase {
    EnvSpec env
    KVMHostInventory host

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(ZStackTest.springSpec)
    }

    @Override
    void environment() {
        env = env {
            zone {
                name = "zone"
                description = "test"

                cluster {
                    name = "cluster"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm"
                        managementIp = "localhost"
                        username = "root"
                        password = "password"
                    }

                }
            }
        }
    }

    @Override
    void test() {
        env.create {
            host = env.inventoryByName("kvm") as KVMHostInventory

            testRestResultWillFilterPasswordWhenUpdateKvmHostInventory()
        }
    }

    void testRestResultWillFilterPasswordWhenUpdateKvmHostInventory() {
        updateKVMHost {
            uuid = host.uuid
            password = "test"
        }

        String ret = Q.New(AsyncRestVO.class)
                .select(AsyncRestVO_.result)
                .like(AsyncRestVO_.requestData, "%\"apiClassName\":\"org.zstack.kvm.APIUpdateKVMHostMsg\"%").findValue()

        // the filed of password won't exists due to @APINoSee annotation
        assert ret.indexOf("password") == -1
        SQL.New(AsyncRestVO.class).delete()
    }
}
