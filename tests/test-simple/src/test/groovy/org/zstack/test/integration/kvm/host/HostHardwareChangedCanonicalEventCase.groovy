package org.zstack.test.integration.kvm.host

import org.zstack.compute.host.HostSystemTags
import org.zstack.core.cloudbus.EventCallback
import org.zstack.core.cloudbus.EventFacade
import org.zstack.header.host.HostCanonicalEvents
import org.zstack.kvm.KVMConstant
import org.zstack.kvm.KVMAgentCommands
import org.zstack.sdk.HostInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

import java.util.concurrent.TimeUnit

class HostHardwareChangedCanonicalEventCase extends SubCase {
    EnvSpec env
    String cpuGHz = "2.10"
    String systemProductName = "product-a"

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = HostEnv.oneHostEnv()
    }

    @Override
    void test() {
        env.afterSimulator(KVMConstant.KVM_HOST_FACT_PATH) { KVMAgentCommands.HostFactResponse rsp ->
            rsp.cpuGHz = cpuGHz
            rsp.systemProductName = systemProductName
            return rsp
        }

        env.create {
            testCpuFrequencyChangeDoesNotFireHardwareChangedEvent()
        }
    }

    void testCpuFrequencyChangeDoesNotFireHardwareChangedEvent() {
        HostInventory host = env.inventoryByName("kvm") as HostInventory
        List<HostCanonicalEvents.HostHardwareChangedData> events = Collections.synchronizedList([])
        EventFacade evtf = bean(EventFacade.class)
        evtf.on(HostCanonicalEvents.HOST_HARDWARE_CHANGED_PATH, new EventCallback() {
            @Override
            protected void run(Map tokens, Object data) {
                HostCanonicalEvents.HostHardwareChangedData event = data as HostCanonicalEvents.HostHardwareChangedData
                if (event.hostUuid == host.uuid) {
                    events.add(event)
                }
            }
        })

        cpuGHz = "2.20"
        reconnectHost {
            uuid = host.uuid
        }

        retryInSecs {
            assert HostSystemTags.CPU_GHZ.getTokenByResourceUuid(
                    host.uuid, HostSystemTags.CPU_GHZ_TOKEN) == cpuGHz
        }
        TimeUnit.SECONDS.sleep(1)
        assert events.isEmpty()

        systemProductName = "product-b"
        reconnectHost {
            uuid = host.uuid
        }

        retryInSecs {
            assert events.size() == 1
        }
        assert events[0].reason.causes.size() == 1
        String details = events[0].reason.causes[0].details
        assert details.contains(HostSystemTags.SYSTEM_PRODUCT_NAME.getTagFormat())
        assert details.contains("old:product-a, new:product-b")
    }

    @Override
    void clean() {
        env.delete()
    }
}
