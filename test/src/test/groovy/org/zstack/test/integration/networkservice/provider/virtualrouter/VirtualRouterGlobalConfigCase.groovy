package org.zstack.test.integration.networkservice.provider.virtualrouter


import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.network.service.virtualrouter.VirtualRouterGlobalConfig
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest


class VirtualRouterGlobalConfigCase extends SubCase {
    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
        useSpring(NetworkServiceProviderTest.springSpec)
    }

    @Override
    void environment() {
        env = env {
            zone {
                name = "zone"
            }
        }
    }

    @Override
    void test() {
        env.create {
            testIpv4LocalPortRange()
        }
    }

    void testIpv4LocalPortRange(){
        expect(AssertionError.class) {
            updateGlobalConfig {
                category = VirtualRouterGlobalConfig.CATEGORY
                name = VirtualRouterGlobalConfig.IPV4_LOCAL_PORT_RANGE.name
                value = "0"
            }
        }

        expect(AssertionError.class) {
            updateGlobalConfig {
                category = VirtualRouterGlobalConfig.CATEGORY
                name = VirtualRouterGlobalConfig.IPV4_LOCAL_PORT_RANGE.name
                value = "20-1"
            }
        }

        expect(AssertionError.class) {
            updateGlobalConfig {
                category = VirtualRouterGlobalConfig.CATEGORY
                name = VirtualRouterGlobalConfig.IPV4_LOCAL_PORT_RANGE.name
                value = "20-70000"
            }
        }

        expect(AssertionError.class) {
            updateGlobalConfig {
                category = VirtualRouterGlobalConfig.CATEGORY
                name = VirtualRouterGlobalConfig.IPV4_LOCAL_PORT_RANGE.name
                value = "1023-2000"
            }
        }

        updateGlobalConfig {
            category = VirtualRouterGlobalConfig.CATEGORY
            name = VirtualRouterGlobalConfig.IPV4_LOCAL_PORT_RANGE.name
            value = "20000-30000"
        }
    }


}
