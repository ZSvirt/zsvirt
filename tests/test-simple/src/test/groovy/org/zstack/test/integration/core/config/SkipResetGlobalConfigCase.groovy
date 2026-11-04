package org.zstack.test.integration.core.config

import org.zstack.core.config.GlobalConfigBeforeResetExtensionPoint
import org.zstack.core.config.SkipResetGlobalConfigException
import org.zstack.header.identity.SessionInventory
import org.zstack.image.ImageGlobalConfig
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

class SkipResetGlobalConfigCase extends SubCase {
    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = makeEnv {
            zone {
                name = "zone"
            }
        }
    }

    @Override
    void test() {
        env.create {
            testBeforeResetGlobalConfigExtensionPoint()
        }
    }

    void testBeforeResetGlobalConfigExtensionPoint() {
        Long defaultValue = Long.valueOf(ImageGlobalConfig.EXPUNGE_INTERVAL.defaultValue)

        updateGlobalConfig {
            category = ImageGlobalConfig.CATEGORY
            name = ImageGlobalConfig.EXPUNGE_INTERVAL.name
            value = defaultValue + 1
        }

        assert ImageGlobalConfig.EXPUNGE_INTERVAL.value(Long.class) != defaultValue
        resetGlobalConfig {}
        assert ImageGlobalConfig.EXPUNGE_INTERVAL.value(Long.class) == defaultValue

        ImageGlobalConfig.EXPUNGE_INTERVAL.installBeforeResetExtension(new GlobalConfigBeforeResetExtensionPoint() {
            @Override
            void beforeResetExtensionPoint(SessionInventory session) {
                throw new SkipResetGlobalConfigException()
            }
        })

        updateGlobalConfig {
            category = ImageGlobalConfig.CATEGORY
            name = ImageGlobalConfig.EXPUNGE_INTERVAL.name
            value = defaultValue + 1
        }

        assert ImageGlobalConfig.EXPUNGE_INTERVAL.value(Long.class) != defaultValue
        resetGlobalConfig {}
        assert ImageGlobalConfig.EXPUNGE_INTERVAL.value(Long.class) != defaultValue

        ImageGlobalConfig.EXPUNGE_INTERVAL.beforeResetExtensions.clear()
    }
}
