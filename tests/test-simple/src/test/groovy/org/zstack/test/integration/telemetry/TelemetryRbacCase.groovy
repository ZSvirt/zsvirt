package org.zstack.test.integration.telemetry

import org.zstack.header.errorcode.SysErrors
import org.zstack.sdk.AccountInventory
import org.zstack.sdk.zsv.telemetry.entity.TelemetryConsentView
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.zsv.telemetry.RBACInfo
import org.zstack.zsv.telemetry.TelemetryConstant
import org.zstack.zsv.telemetry.TelemetryErrors
import org.zstack.zsv.telemetry.header.TelemetryGlobalConfig

class TelemetryRbacCase extends SubCase {
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
            prepare()
            testTelemetryRbacAtRuntime()
        }
    }

    void prepare() {
        def grantedAt = TelemetryGlobalConfig.CONSENT_GRANTED_AT.value()
        if (grantedAt != TelemetryConstant.CONSENT_NOT_GRANTED) {
            updateTelemetryConsent {
                delegate.action = TelemetryConstant.CONSENT_ACTION_DISABLED
            }
        }

        retryInSecs {
            assert TelemetryGlobalConfig.CONSENT_GRANTED_AT.value() == TelemetryConstant.CONSENT_NOT_GRANTED
        }
    }

    void testTelemetryRbacAtRuntime() {
        logger.info("Test 001: create normal account with other role only, expect read success")
        AccountInventory account = createAccount {
            delegate.name = "telemetry-rbac-read"
            delegate.password = "password"
        } as AccountInventory

        withAccountSession("telemetry-rbac-read", "password") {
            TelemetryConsentView consent = getTelemetryConsent {} as TelemetryConsentView
            assert consent.consentGrantedAt == TelemetryConstant.CONSENT_NOT_GRANTED
            getTelemetrySetting {}
        }

        logger.info("Test 002: account without telemetry role, expect update consent failure OPERATION_DENIED")
        withAccountSession("telemetry-rbac-read", "password") {
            expectApiFailure({
                updateTelemetryConsent {
                    delegate.action = TelemetryConstant.CONSENT_ACTION_DISABLED
                }
            }) {
                assert delegate.code == SysErrors.OPERATION_DENIED.toString()
            }
        }

        logger.info("Test 002b: admin UpdateGlobalConfig on consent.granted.at forbidden, expect TELEMETRY.2003")
        expectApiFailure({
            updateGlobalConfig {
                delegate.category = TelemetryGlobalConfig.CATEGORY
                delegate.name = TelemetryGlobalConfig.CONSENT_GRANTED_AT.name
                delegate.value = TelemetryConstant.CONSENT_NOT_GRANTED
            }
        }) {
            assert delegate.code == TelemetryErrors.TELEMETRY_CONSENT_UPDATE_FORBIDDEN.toString()
        }

        logger.info("Test 003: attach predefined telemetry role, expect update consent API allowed by RBAC")
        attachRoleToAccount {
            delegate.accountUuid = account.uuid
            delegate.roleUuid = RBACInfo.TELEMETRY_ROLE_UUID
        }

        withAccountSession("telemetry-rbac-read", "password") {
            updateTelemetryConsent {
                delegate.action = TelemetryConstant.CONSENT_ACTION_ENABLED
                delegate.agreedToTerms = true
            }
        }

        logger.info("Test 099: clean consent")
        updateTelemetryConsent {
            delegate.action = TelemetryConstant.CONSENT_ACTION_DISABLED
        }
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
