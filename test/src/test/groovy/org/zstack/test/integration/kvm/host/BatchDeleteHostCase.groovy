package org.zstack.test.integration.kvm.host

import org.zstack.core.db.Q
import org.zstack.header.host.HostVO
import org.zstack.sdk.ClusterInventory
import org.zstack.sdk.DeleteHostAction
import org.zstack.sdk.HostInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.Test

import java.util.concurrent.atomic.AtomicInteger

/**
 *  Created by lining on 2020/4/21
 *
 */

class BatchDeleteHostCase extends SubCase {
    EnvSpec env

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = HostEnv.noHostBasicEnv()
    }

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void test() {
        env.create {
            testBatchDeleteHost()
        }
    }

    void testBatchDeleteHost() {
        ClusterInventory cluster = env.inventoryByName("cluster")

        List<String> hostUuids = []
        for (int i = 2; i < 52; i++) {
            def ip = String.format("127.0.0.%d", i)

            HostInventory host = addKVMHost {
                username = "test"
                password = "password"
                name = "host"
                managementIp = ip
                clusterUuid = cluster.uuid
            }

            hostUuids.add(host.uuid)
        }

        AtomicInteger count = new AtomicInteger(0)
        def threads = []

        for (String hostUuid : hostUuids) {
            String uuid = hostUuid
            def thread = Thread.start {
                DeleteHostAction action = new DeleteHostAction(
                        uuid: uuid,
                        sessionId: Test.currentEnvSpec.session.uuid,
                )
                action.call()
                count.incrementAndGet()
            }

            threads.add(thread)
        }

        threads.each { it.join() }

        retryInSecs(15, 3) {
            assert count.get() == hostUuids.size()
        }

        assert Q.New(HostVO.class).count() == 0L
    }
}
