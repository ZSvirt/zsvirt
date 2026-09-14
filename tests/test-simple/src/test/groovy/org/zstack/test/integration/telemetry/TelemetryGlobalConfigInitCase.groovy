package org.zstack.test.integration.telemetry

import org.apache.commons.codec.digest.DigestUtils
import org.zstack.core.Platform
import org.zstack.core.config.GlobalConfigFacadeImpl
import org.zstack.sdk.GlobalConfigInventory
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.zsv.telemetry.TelemetryConstant
import org.zstack.zsv.telemetry.TelemetryErrors
import org.zstack.zsv.telemetry.header.TelemetryGlobalConfig
import org.zstack.zsv.telemetry.privacy.TelemetryAnonymizer

class TelemetryGlobalConfigInitCase extends SubCase {
    EnvSpec env

    @Override
    void setup() {
        useSpring(TelemetryTest.springSpec)
    }

    @Override
    void environment() {
        env = makeEnv {
        }
    }

    @Override
    void test() {
        env.create {
            testUpdateGlobalConfig()
            testGlobalConfigInit()
            testAnonymizer()
        }
    }

    void testUpdateGlobalConfig() {
        logger.info("Test 101: cloud.base.url only support https")
        updateGlobalConfig {
            delegate.category = TelemetryGlobalConfig.CATEGORY
            delegate.name = TelemetryGlobalConfig.CLOUD_BASE_URL.name
            delegate.value = "https://www.zstack.io"
        }

        expectApiFailure({
            updateGlobalConfig {
                delegate.category = TelemetryGlobalConfig.CATEGORY
                delegate.name = TelemetryGlobalConfig.CLOUD_BASE_URL.name
                delegate.value = "http://www.zstack.io"
            }
        }) {
            assert delegate.code == "SYS.1007"
        }
    }

    void testGlobalConfigInit() {
        logger.info("Test 401: verify telemetry source.id initialized with management node uuid")
        String managementNodeUuid = Platform.getManagementServerId()
        assert managementNodeUuid != null

        List<GlobalConfigInventory> sourceConfigs = queryGlobalConfig {
            delegate.conditions = ["category=${TelemetryGlobalConfig.CATEGORY}", "name=${TelemetryGlobalConfig.SOURCE_ID.name}"]
        } as List<GlobalConfigInventory>
        assert sourceConfigs.size() == 1
        assert sourceConfigs[0].value == managementNodeUuid
        assert TelemetryGlobalConfig.SOURCE_ID.value() == managementNodeUuid

        logger.info("Test 402: verify telemetry anonymization.salt initialized with random 64-char hex value")
        List<GlobalConfigInventory> saltConfigs = queryGlobalConfig {
            delegate.conditions = ["category=${TelemetryGlobalConfig.CATEGORY}", "name=${TelemetryGlobalConfig.ANONYMIZATION_SALT.name}"]
        } as List<GlobalConfigInventory>
        assert saltConfigs.size() == 1
        String salt = saltConfigs[0].value
        assert salt != TelemetryConstant.CONSENT_NOT_GRANTED
        assert salt == TelemetryGlobalConfig.ANONYMIZATION_SALT.value()
        assert salt ==~ /[0-9a-f]{64}/

        logger.info("Test 403: reload global config facade, expect source.id and salt unchanged")
        String sourceBefore = TelemetryGlobalConfig.SOURCE_ID.value()
        String saltBefore = TelemetryGlobalConfig.ANONYMIZATION_SALT.value()
        bean(GlobalConfigFacadeImpl.class).start()
        assert TelemetryGlobalConfig.SOURCE_ID.value() == sourceBefore
        assert TelemetryGlobalConfig.ANONYMIZATION_SALT.value() == saltBefore

        logger.info("Test 404: UpdateGlobalConfig on initialized source.id and anonymization.salt forbidden, expect TELEMETRY.2004")
        expectApiFailure({
            updateGlobalConfig {
                delegate.category = TelemetryGlobalConfig.CATEGORY
                delegate.name = TelemetryGlobalConfig.SOURCE_ID.name
                delegate.value = Platform.getUuid()
            }
        }) {
            assert code == TelemetryErrors.TELEMETRY_READONLY_GLOBAL_CONFIG.toString()
        }

        expectApiFailure({
            updateGlobalConfig {
                delegate.category = TelemetryGlobalConfig.CATEGORY
                delegate.name = TelemetryGlobalConfig.ANONYMIZATION_SALT.name
                delegate.value = Platform.getUuid() + Platform.getUuid()
            }
        }) {
            assert code == TelemetryErrors.TELEMETRY_READONLY_GLOBAL_CONFIG.toString()
        }

        assert TelemetryGlobalConfig.SOURCE_ID.value() == sourceBefore
        assert TelemetryGlobalConfig.ANONYMIZATION_SALT.value() == saltBefore
    }

    void testAnonymizer() {
        logger.info("Test 501: anonymize uuid with configured salt, expect stable 64-char lowercase hex")
        TelemetryAnonymizer anonymizer = bean(TelemetryAnonymizer.class)
        assert anonymizer.isSaltInitialized()

        String uuid = Platform.getUuid()
        String anonymized = anonymizer.anonymize(uuid)
        assert anonymized == anonymizer.anonymize(uuid)
        assert anonymized ==~ /[0-9a-f]{64}/
        assert anonymized == DigestUtils.sha256Hex(uuid + TelemetryGlobalConfig.ANONYMIZATION_SALT.value())

        logger.info("Test 502: anonymize with explicit salt, expect different output from configured salt")
        String otherSalt = "other-salt"
        String withOtherSalt = anonymizer.anonymize(uuid, otherSalt)
        assert withOtherSalt != anonymized
        assert withOtherSalt == DigestUtils.sha256Hex(uuid + otherSalt)
    }

    @Override
    void clean() {
        env.delete()
    }
}
