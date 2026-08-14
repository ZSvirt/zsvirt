package org.zstack.test.integration.core.config

import org.zstack.core.Platform
import org.zstack.core.cloudbus.EventFacade
import org.zstack.core.config.GlobalConfig
import org.zstack.core.config.GlobalConfigBeforeResetExtensionPoint
import org.zstack.core.config.GlobalConfigCanonicalEvents
import org.zstack.core.config.GlobalConfigFacadeImpl
import org.zstack.core.config.GlobalConfigVO
import org.zstack.core.config.GlobalConfigVO_
import org.zstack.core.config.SkipResetGlobalConfigException
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.core.db.SimpleQuery
import org.zstack.core.db.UpdateQuery
import org.zstack.header.identity.SessionInventory
import org.zstack.header.vm.APICreateVmNicMsg
import org.zstack.image.ImageGlobalConfig
import org.zstack.kvm.KVMGlobalConfig
import org.zstack.sdk.GlobalConfigInventory
import org.zstack.sdk.UpdateGlobalConfigAction
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

import static org.zstack.utils.CollectionDSL.e
import static org.zstack.utils.CollectionDSL.map
import static org.zstack.utils.StringDSL.s

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
        env = env {
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
