package org.zstack.test.integration.telemetry

import org.zstack.sdk.GlobalConfigInventory
import org.zstack.sdk.zsv.telemetry.entity.TelemetryConsentView
import org.zstack.sdk.zsv.telemetry.entity.TelemetrySettingView
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.zsv.telemetry.TelemetryConstant
import org.zstack.zsv.telemetry.TelemetryErrors
import org.zstack.zsv.telemetry.header.TelemetryGlobalConfig

class TelemetryConsentCase extends SubCase {
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
            testGetDefaultConsent()
            testEnableConsent()
            testEnableConsentFailure()
            testDisableConsent()
        }
    }

    void testGetDefaultConsent() {
        logger.info("Test 001: get telemetry consent, expect default None")
        TelemetryConsentView consent = getTelemetryConsent {} as TelemetryConsentView
        assert consent.consentGrantedAt == TelemetryConstant.CONSENT_NOT_GRANTED

        List<GlobalConfigInventory> configs = queryGlobalConfig {
            delegate.conditions = ["category=${TelemetryGlobalConfig.CATEGORY}", "name=${TelemetryGlobalConfig.CONSENT_GRANTED_AT.name}"]
        } as List<GlobalConfigInventory>
        assert configs.size() == 1
        assert configs[0].value == TelemetryConstant.CONSENT_NOT_GRANTED

        logger.info("Test 002: get telemetry setting, expect description key and privacy policy url")
        TelemetrySettingView setting = getTelemetrySetting {} as TelemetrySettingView
        assert setting.descriptionKey == TelemetryConstant.SETTING_DESCRIPTION_I18N_KEY
        assert setting.privacyPolicyUrl == TelemetryConstant.SETTING_PRIVACY_POLICY_URL
    }

    void testEnableConsent() {
        logger.info("Test 101: enable telemetry consent, expect UTC timestamp and scheduler started")
        def consent101 = updateTelemetryConsent {
            delegate.action = TelemetryConstant.CONSENT_ACTION_ENABLED
            delegate.agreedToTerms = true
        } as TelemetryConsentView

        assert consent101.consentGrantedAt != TelemetryConstant.CONSENT_NOT_GRANTED
        assert consent101.consentGrantedAt.endsWith("Z")
        assert consent101.consentGrantedAt ==~ /\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}Z/

        def configs101 = queryGlobalConfig {
            delegate.conditions = ["category=${TelemetryGlobalConfig.CATEGORY}", "name=${TelemetryGlobalConfig.CONSENT_GRANTED_AT.name}"]
        } as List<GlobalConfigInventory>
        assert configs101.size() == 1
        assert configs101[0].value == consent101.consentGrantedAt

        logger.info("Test 102: disable telemetry consent")
        def consent102 = updateTelemetryConsent {
            delegate.action = TelemetryConstant.CONSENT_ACTION_DISABLED
        } as TelemetryConsentView
        assert consent102.consentGrantedAt == TelemetryConstant.CONSENT_NOT_GRANTED

        def configs102 = queryGlobalConfig {
            delegate.conditions = ["category=${TelemetryGlobalConfig.CATEGORY}", "name=${TelemetryGlobalConfig.CONSENT_GRANTED_AT.name}"]
        } as List<GlobalConfigInventory>
        assert configs102.size() == 1
        assert configs102[0].value == consent102.consentGrantedAt

        logger.info("Test 103: enable telemetry consent again")
        def consent103 = updateTelemetryConsent {
            delegate.action = TelemetryConstant.CONSENT_ACTION_ENABLED
            delegate.agreedToTerms = true
        } as TelemetryConsentView
        assert consent103.consentGrantedAt != TelemetryConstant.CONSENT_NOT_GRANTED
        assert consent103.consentGrantedAt.endsWith("Z")
        assert consent103.consentGrantedAt ==~ /\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}Z/

        def configs103 = queryGlobalConfig {
            delegate.conditions = ["category=${TelemetryGlobalConfig.CATEGORY}", "name=${TelemetryGlobalConfig.CONSENT_GRANTED_AT.name}"]
        } as List<GlobalConfigInventory>
        assert configs103.size() == 1
        assert configs103[0].value == consent103.consentGrantedAt

        logger.info("Test 199: clean")
    }

    void testEnableConsentFailure() {
        logger.info("Test 201: enable telemetry consent again, expect API failure TELEMETRY.2002")
        expectApiFailure({
            updateTelemetryConsent {
                delegate.action = TelemetryConstant.CONSENT_ACTION_ENABLED
                delegate.agreedToTerms = true
            }
        }) {
            assert delegate.code == TelemetryErrors.TELEMETRY_ALREADY_ENABLED.toString()
        }

        logger.info("Test 202: disable consent before testing missing agreedToTerms")
        updateTelemetryConsent {
            delegate.action = TelemetryConstant.CONSENT_ACTION_DISABLED
        }

        logger.info("Test 203: enable telemetry consent without agreedToTerms, expect API failure TELEMETRY.2001")
        expectApiFailure({
            updateTelemetryConsent {
                delegate.action = TelemetryConstant.CONSENT_ACTION_ENABLED
            }
        }) {
            assert delegate.code == TelemetryErrors.TELEMETRY_CONSENT_REQUIRED.toString()
        }

        List<GlobalConfigInventory> configs = queryGlobalConfig {
            delegate.conditions = ["category=${TelemetryGlobalConfig.CATEGORY}", "name=${TelemetryGlobalConfig.CONSENT_GRANTED_AT.name}"]
        } as List<GlobalConfigInventory>
        assert configs.size() == 1
        assert configs[0].value == TelemetryConstant.CONSENT_NOT_GRANTED
    }

    void testDisableConsent() {
        logger.info("Test 301: enable telemetry consent for disable test, expect success")
        updateTelemetryConsent {
            delegate.action = TelemetryConstant.CONSENT_ACTION_ENABLED
            delegate.agreedToTerms = true
        }

        List<GlobalConfigInventory> configs = queryGlobalConfig {
            delegate.conditions = ["category=${TelemetryGlobalConfig.CATEGORY}", "name=${TelemetryGlobalConfig.CONSENT_GRANTED_AT.name}"]
        } as List<GlobalConfigInventory>
        assert configs.size() == 1
        assert configs[0].value != TelemetryConstant.CONSENT_NOT_GRANTED

        logger.info("Test 302: disable telemetry consent, expect None and scheduler stopped")
        TelemetryConsentView consent = updateTelemetryConsent {
            delegate.action = TelemetryConstant.CONSENT_ACTION_DISABLED
        } as TelemetryConsentView

        assert consent.consentGrantedAt == TelemetryConstant.CONSENT_NOT_GRANTED

        configs = queryGlobalConfig {
            delegate.conditions = ["category=${TelemetryGlobalConfig.CATEGORY}", "name=${TelemetryGlobalConfig.CONSENT_GRANTED_AT.name}"]
        } as List<GlobalConfigInventory>
        assert configs.size() == 1
        assert configs[0].value == TelemetryConstant.CONSENT_NOT_GRANTED

        logger.info("Test 399: clean telemetry consent, expect None")
        updateTelemetryConsent {
            delegate.action = TelemetryConstant.CONSENT_ACTION_DISABLED
        }
        consent = getTelemetryConsent {} as TelemetryConsentView
        assert consent.consentGrantedAt == TelemetryConstant.CONSENT_NOT_GRANTED
    }

    @Override
    void clean() {
        try {
            updateTelemetryConsent {
                delegate.action = TelemetryConstant.CONSENT_ACTION_DISABLED
            }
        } catch (AssertionError ignored) {}

        env.delete()
    }
}
