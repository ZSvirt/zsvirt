package org.zstack.test.integration.kvm.host

import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.db.Q
import org.zstack.header.host.ConnectHostMsg
import org.zstack.header.host.ConnectHostReply
import org.zstack.header.host.HostVO
import org.zstack.sdk.ClusterInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

import static org.zstack.core.Platform.operr

class BatchAddHostCase extends SubCase {
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
    void test() {
        env.create {
            testBatchAddHost()
        }
    }

    void testBatchAddHost() {
        boolean over = false
        env.message(ConnectHostMsg.class) { ConnectHostMsg msg, CloudBus bus ->
            ConnectHostReply reply = new ConnectHostReply()
            while (!over) {
                sleep(1000)
            }
            reply.setError(operr("on purpose"))
            bus.reply(msg, reply)
        }

        def cluster = env.inventoryByName("cluster") as ClusterInventory

        def threads = []
        1.upto(31, { it ->
            def vmName = "test-${it}"
            def ip = "127.0.0.${it}"
            def thread = Thread.start {
                addKVMHost {
                    name = vmName
                    managementIp = ip
                    username = "root"
                    password = "password"
                    clusterUuid = cluster.uuid
                }
            }

            threads.add(thread)
        })

        sleep(1000)
        retryInSecs {
            assert Q.New(HostVO.class).count() == 30
        }

        over = true
        threads.each{it.join()}
        assert Q.New(HostVO.class).count() == 0
    }

    @Override
    void clean() {
        env.delete()
    }
}
