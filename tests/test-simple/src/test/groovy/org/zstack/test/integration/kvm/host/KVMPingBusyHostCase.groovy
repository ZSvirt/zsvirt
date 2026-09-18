package org.zstack.test.integration.kvm.host

import org.springframework.http.HttpEntity
import org.zstack.compute.host.HostGlobalConfig
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.SizeUtils

import static org.zstack.kvm.KVMGlobalConfig.*

class KVMPingBusyHostCase extends SubCase {
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

                cluster {
                    name = "cluster"

                    kvm {
                        name = "kvm1"
                        managementIp = "127.0.0.1"
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
            prepare()
            testHostPingConfig()
        }
    }

    void prepare() {
        updateGlobalConfig {
            delegate.category = HostGlobalConfig.PING_HOST_INTERVAL.category
            delegate.name = HostGlobalConfig.PING_HOST_INTERVAL.name
            delegate.value = "1"
        }

        updateGlobalConfig {
            delegate.category = HostGlobalConfig.MAXIMUM_PING_FAILURE.category
            delegate.name = HostGlobalConfig.MAXIMUM_PING_FAILURE.name
            delegate.value = "1"
        }

        updateGlobalConfig {
            delegate.category = HostGlobalConfig.SLEEP_TIME_AFTER_PING_FAILURE.category
            delegate.name = HostGlobalConfig.SLEEP_TIME_AFTER_PING_FAILURE.name
            delegate.value = "0"
        }
    }

    void testHostPingConfig() {
        logger.info("Test 001: mem_hard_limit=10G, mem_threshold=2G")

        def kvmAgentMemThreshold = null
        def kvmAgentMemHardLimit = null
        env.afterSimulator(KVMConstant.KVM_PING_PATH) { KVMAgentCommands.PingResponse rsp, HttpEntity<String> e ->
            def cmd = json(e.getBody(), KVMAgentCommands.PingCmd)
            kvmAgentMemThreshold = cmd.configs[KVMAGENT_PHYSICAL_MEMORY_USAGE_ALARM_THRESHOLD.getName()]
            kvmAgentMemHardLimit = cmd.configs[KVMAGENT_PHYSICAL_MEMORY_USAGE_HARD_LIMIT.getName()]
            return rsp
        }

        retryInSecs {
            assert kvmAgentMemThreshold == SizeUtils.sizeStringToBytes("2G")
            assert kvmAgentMemHardLimit == SizeUtils.sizeStringToBytes("10G")
        }

        logger.info("Test 002: mem_hard_limit=200M, mem_threshold=100M")
        updateGlobalConfig {
            category = KVMAGENT_PHYSICAL_MEMORY_USAGE_ALARM_THRESHOLD.category
            name = KVMAGENT_PHYSICAL_MEMORY_USAGE_ALARM_THRESHOLD.name
            value = SizeUtils.sizeStringToBytes("100M")
        }

        updateGlobalConfig {
            category = KVMAGENT_PHYSICAL_MEMORY_USAGE_HARD_LIMIT.category
            name = KVMAGENT_PHYSICAL_MEMORY_USAGE_HARD_LIMIT.name
            value = SizeUtils.sizeStringToBytes("200M")
        }

        retryInSecs {
            assert kvmAgentMemThreshold == SizeUtils.sizeStringToBytes("100M")
            assert kvmAgentMemHardLimit == SizeUtils.sizeStringToBytes("200M")
        }


        logger.info("Test 003: mem_hard_limit=5G, mem_threshold=20G")
        updateGlobalConfig {
            category = KVMAGENT_PHYSICAL_MEMORY_USAGE_ALARM_THRESHOLD.category
            name = KVMAGENT_PHYSICAL_MEMORY_USAGE_ALARM_THRESHOLD.name
            value = SizeUtils.sizeStringToBytes("20G")
        }

        updateGlobalConfig {
            category = KVMAGENT_PHYSICAL_MEMORY_USAGE_HARD_LIMIT.category
            name = KVMAGENT_PHYSICAL_MEMORY_USAGE_HARD_LIMIT.name
            value = SizeUtils.sizeStringToBytes("5G")
        }

        retryInSecs {
            assert kvmAgentMemThreshold == SizeUtils.sizeStringToBytes("20G")
            assert kvmAgentMemHardLimit == SizeUtils.sizeStringToBytes("5G")
        }
    }

    @Override
    void clean() {
        env.delete()
    }
}
