package org.zstack.test.integration.network.l3network

import org.zstack.sdk.ApiException
import org.zstack.sdk.L2NetworkInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.test.integration.network.NetworkTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

/**
 * Created by weiwang on 20/09/2017
 */
class L3NetworkCategoryCase extends SubCase {
    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(NetworkTest.springSpec)
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.OneIpL3Network()
    }

    @Override
    void test() {
        env.create {
            testCreate()
            testUpdate()
        }
    }

    void testCreate() {
        L3NetworkInventory l3 = env.inventoryByName("l3")
        L2NetworkInventory l2 = env.inventoryByName("l2")
        assert l3.category == "Private"

        expect(AssertionError.class) {
            createL3Network {
                delegate.l2NetworkUuid = l2.uuid
                delegate.name = "test-l3-1"
                delegate.system = true
                delegate.category = "Private"
            }
        }

        expect(ApiException.class) {
            createL3Network {
                delegate.l2NetworkUuid = l2.uuid
                delegate.name = "test-l3-1"
                delegate.system = true
                delegate.category = "system-1"
            }
        }

        expect(AssertionError.class) {
            createL3Network {
                delegate.l2NetworkUuid = l2.uuid
                delegate.name = "test-l3-1"
                delegate.category = "System"
            }
        }

        expect(AssertionError.class) {
            createL3Network {
                delegate.l2NetworkUuid = l2.uuid
                delegate.name = "test-l3-1"
                delegate.system = false
                delegate.category = "System"
            }
        }

        L3NetworkInventory test_l3_1 = createL3Network {
            delegate.l2NetworkUuid = l2.uuid
            delegate.name = "test-l3-1"
            delegate.system = true
            delegate.category = "System"
        }

        deleteL3Network {
            delegate.uuid = test_l3_1.uuid
        }
    }

    void testUpdate() {
        L3NetworkInventory l3 = env.inventoryByName("l3")

        expect(AssertionError.class) {
            updateL3Network {
                delegate.uuid = l3.uuid
                delegate.system = true
                delegate.category = "Private"
            }
        }

        expect(AssertionError.class) {
            updateL3Network {
                delegate.uuid = l3.uuid
                delegate.system = true
            }
        }

        expect(AssertionError.class) {
            updateL3Network {
                delegate.uuid = l3.uuid
                delegate.system = false
                delegate.category = "System"
            }
        }

        expect(AssertionError.class) {
            updateL3Network {
                delegate.uuid = l3.uuid
                delegate.category = "System"
            }
        }

        updateL3Network {
            delegate.uuid = l3.uuid
            delegate.category = "Public"
        }
    }
}
