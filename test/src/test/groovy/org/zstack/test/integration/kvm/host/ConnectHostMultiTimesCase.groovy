package org.zstack.test.integration.kvm.host

import org.springframework.http.HttpEntity
import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.cloudbus.CloudBusCallBack
import org.zstack.core.db.Q
import org.zstack.core.timeout.ApiTimeoutGlobalProperty
import org.zstack.header.host.ConnectHostMsg
import org.zstack.header.host.HostConstant
import org.zstack.header.host.HostStateEvent
import org.zstack.header.host.HostStatus
import org.zstack.header.host.HostVO
import org.zstack.header.host.HostVO_
import org.zstack.header.host.ReconnectHostMsg
import org.zstack.header.message.MessageReply
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.sdk.HostInventory
import org.zstack.test.integration.kvm.Env
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer

/**
 * Created by mingjian.deng on 2019/10/16.
 */
class ConnectHostMultiTimesCase extends SubCase {
    EnvSpec env
    HostInventory host1
    HostInventory host2
    CloudBus bus

    AtomicInteger caller = new AtomicInteger(0)

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.noVmTwoHostsEnv()
    }

    @Override
    void test() {
        bus = bean(CloudBus.class)
        env.create {
            host1 = env.inventoryByName("kvm1") as HostInventory
            host2 = env.inventoryByName("kvm2") as HostInventory

            env.afterSimulator(KVMConstant.KVM_CONNECT_PATH) { KVMAgentCommands.AgentResponse rsp, HttpEntity<String> e ->
                sleep 1000
                caller.incrementAndGet()
                return rsp
            }

            testConnectHost(100)
            testConnectTwoHosts(50, 50)
            env.cleanAfterSimulatorHandlers()
            
            testReconnectFail(30)
            testConnectTimeout(30)
            testHostOutOfMaintainStateWillReconnectHost()
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    void testHostOutOfMaintainStateWillReconnectHost() {
        changeHostState {
            uuid = host1.uuid
            stateEvent = "maintain"
        }
        CountDownLatch latch = new CountDownLatch(1)
        AtomicInteger successCount = new AtomicInteger(0)
        AtomicInteger errorCount = new AtomicInteger(0)

        sendConnectHostInternalMessage(host1.uuid, { reply ->
            if (reply.isSuccess()) {
                successCount.incrementAndGet()
            } else if (reply.error != null) {
                errorCount.incrementAndGet()
            }
            latch.countDown()
        })
        latch.await()
        assert successCount.get() == 0
        assert errorCount.get() == 1

        def count = 0
        def cleanup = notifyWhenReceivedMessage(ReconnectHostMsg.class) { ReconnectHostMsg msg ->
            if (msg.hostUuid == host1.uuid) {
                count++
            }
        }

        changeHostState {
            uuid = host1.uuid
            stateEvent = HostStateEvent.enable
        }

        assert count == 1
        cleanup()

        retryInSecs {
            assert Q.New(HostVO.class).eq(HostVO_.uuid, host1.uuid).select(HostVO_.status).findValue() == HostStatus.Connected
        }
    }

    void testConnectHost(int times) {
        CountDownLatch latch = new CountDownLatch(times)
        AtomicInteger successCount = new AtomicInteger(0)
        AtomicInteger cancelCount = new AtomicInteger(0)
        caller.set(0)

        for (int i in 1..times) {
            sendConnectHostInternalMessage(host1.uuid, { reply ->
                if (reply.isSuccess()) {
                    successCount.incrementAndGet()
                } else if (reply.isCanceled()) {
                    cancelCount.incrementAndGet()
                }
                latch.countDown()
            })
        }

        latch.await()
        assert caller.get() == 1
        assert successCount.get() == times
        assert cancelCount.get() == 0

        refreshHost()
        assert host1.status == HostStatus.Connected.toString()
        assert host2.status == HostStatus.Connected.toString()
    }

    void testConnectTwoHosts(int times1, int times2) {
        CountDownLatch latch = new CountDownLatch(times1 + times2)
        AtomicInteger successCount1 = new AtomicInteger(0)
        AtomicInteger cancelCount1 = new AtomicInteger(0)
        AtomicInteger successCount2 = new AtomicInteger(0)
        AtomicInteger cancelCount2 = new AtomicInteger(0)
        caller.set(0)

        for (int i in 1..times1) {
            sendConnectHostInternalMessage(host1.uuid, { reply ->
                if (reply.isSuccess()) {
                    successCount1.incrementAndGet()
                } else if (reply.isCanceled()) {
                    cancelCount1.incrementAndGet()
                }
                latch.countDown()
            })
        }

        for (int i in 1..times2) {
            sendConnectHostInternalMessage(host2.uuid, { reply ->
                if (reply.isSuccess()) {
                    successCount2.incrementAndGet()
                } else if (reply.isCanceled()) {
                    cancelCount2.incrementAndGet()
                }
                latch.countDown()
            })
        }

        latch.await()
        assert caller.get() == 2
        assert successCount1.get() == times1
        assert cancelCount1.get() == 0
        assert successCount2.get() == times2
        assert cancelCount2.get() == 0

        refreshHost()
        assert host1.status == HostStatus.Connected.toString()
        assert host2.status == HostStatus.Connected.toString()
    }

    /**
     * 问题来源: ZSTAC-31304
     */
    void testReconnectFail(int times) {
        env.simulator(KVMConstant.KVM_CONNECT_PATH) {
            sleep 3000
            caller.incrementAndGet()
            throw new Exception("on purpose")
        }
        
        AtomicInteger failCount = new AtomicInteger(0)
        Runnable r = {
            expect(AssertionError.class) {
                reconnectHost {
                    uuid = host1.uuid
                }
            }
            failCount.incrementAndGet()
        }

        caller.set(0)
        1.upto(times) {
            new Thread(r).start() // 子线程工作
        }
        sleep 100
        r.run() // 主线程工作

        assert caller.get() == 1
        retryInSecs {
            assert failCount.get() == (times + 1) // times 个子线程, 一个主线程
        }
        
        env.cleanSimulatorHandlers()
        
        // 下面说明 reconnectHost 失败一次后仍然能够成功
        reconnectHost {
            uuid = host1.uuid
        }
    }
    
    void testConnectTimeout(int times) {
        CountDownLatch latch = new CountDownLatch(times)
        AtomicInteger successCount = new AtomicInteger(0)
        AtomicInteger errorCount = new AtomicInteger(0)
        AtomicInteger errorMatched = new AtomicInteger(0)
        
        String oldValue = ApiTimeoutGlobalProperty.INTERNAL_MESSAGE_TIMEOUT
        ApiTimeoutGlobalProperty.INTERNAL_MESSAGE_TIMEOUT = "2s"
        
        try {
            env.afterSimulator(KVMConstant.KVM_CONNECT_PATH) {
                sleep 3000 // 3 秒超过 2 秒, 会引发超时
            }

            for (int i in 1..times) {
                sendConnectHostInternalMessage(host1.uuid, { reply ->
                    if (reply.isSuccess()) {
                        successCount.incrementAndGet()
                    } else if (reply.error != null) {
                        errorCount.incrementAndGet()
                        if (reply.error.description.contains("timeout")) {
                            errorMatched.incrementAndGet()
                        }
                    }
                    latch.countDown()
                })
            }

            latch.await()
            assert successCount.get() == 0
            assert errorCount.get() == times
            assert errorMatched.get() == times
        } finally {
            env.cleanAfterSimulatorHandlers()
            ApiTimeoutGlobalProperty.INTERNAL_MESSAGE_TIMEOUT = oldValue

            // 考虑到 MN 会周期性地向 host 发起 connect 请求
            // 现在等待这些可能的请求结束响应
            sleep 2000
        }
        
        // 前面超时结束之后, 再发起 connect 请求应当成功
        successCount.set(0)
        errorCount.set(0)
        latch = new CountDownLatch(1)
        
        sendConnectHostInternalMessage(host1.uuid, { reply ->
            if (reply.isSuccess()) {
                successCount.incrementAndGet()
            } else {
                errorCount.incrementAndGet()
            }
            latch.countDown()
        })

        latch.await()
        assert successCount.get() == 1
        assert errorCount.get() == 0

        refreshHost()
        assert host1.status == HostStatus.Connected.toString()
    }

    void refreshHost() {
        host1 = (queryHost {
            conditions = ["uuid=${host1.uuid}"]
        } as List<HostInventory>)[0]
        host2 = (queryHost {
            conditions = ["uuid=${host2.uuid}"]
        } as List<HostInventory>)[0]
    }
    
    void sendConnectHostInternalMessage(String hostUuid, Consumer<MessageReply> callback) {
        def msg = new ConnectHostMsg()
        msg.uuid = hostUuid
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid)
        bus.send(msg, new CloudBusCallBack(msg) {
            @Override
            void run(MessageReply reply) {
                callback.accept(reply)
            }
        })
    }
}
