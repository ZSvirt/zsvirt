package org.zstack.test.integration.telemetry

import org.springframework.http.HttpMethod
import org.zstack.header.errorcode.ErrorableValue
import org.zstack.sdk.zsv.telemetry.entity.TelemetryConsentView
import org.zstack.sdk.zsv.telemetry.entity.TelemetryUpdateInfoView
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.zsv.telemetry.TelemetryHttpClientSimulator
import org.zstack.testlib.zsv.telemetry.TelemetryVirtualEndpointSpec
import org.zstack.utils.gson.JSONObjectUtil
import org.zstack.zsv.telemetry.TelemetryConstant
import org.zstack.zsv.telemetry.TelemetryErrors

import static org.zstack.core.Platform.operr

class TelemetryCheckUpdateCase extends SubCase {
    EnvSpec env
    TelemetryVirtualEndpointSpec telemetryEndpoint

    @Override
    void setup() {
        useSpring(TelemetryTest.springSpec)
    }

    @Override
    void environment() {
        env = makeEnv {
            telemetryEndpoint {
                endpointName = "telemetry-check-update"
            }
        }
    }

    @Override
    void test() {
        env.create {
            prepare()
            testCheckUpdateSuccessWithoutConsent()
            testCheckUpdateHealthFailure()
            testCheckUpdateInvalidResponse()
            testCheckUpdateMissingVersion()
        }
    }

    void prepare() {
        telemetryEndpoint = env.children.find { it instanceof TelemetryVirtualEndpointSpec } as TelemetryVirtualEndpointSpec
        assert telemetryEndpoint != null
        assert telemetryEndpoint.tempRootDir != null

        TelemetryConsentView consent = getTelemetryConsent {} as TelemetryConsentView
        if (consent.consentGrantedAt != TelemetryConstant.CONSENT_NOT_GRANTED) {
            updateTelemetryConsent {
                delegate.action = TelemetryConstant.CONSENT_ACTION_DISABLED
            }
        }
        consent = getTelemetryConsent {} as TelemetryConsentView
        assert consent.consentGrantedAt == TelemetryConstant.CONSENT_NOT_GRANTED
    }

    void testCheckUpdateSuccessWithoutConsent() {
        logger.info("Test 001: check telemetry update without consent, expect success and cloud request body")
        telemetryEndpoint.checkUpdateBodies.clear()

        TelemetryUpdateInfoView info = checkTelemetryUpdate {} as TelemetryUpdateInfoView
        assert info != null
        assert info.version == "5.1.0"
        assert info.releaseNotesZh != null && !info.releaseNotesZh.isEmpty()
        assert info.releaseNotesEn != null && !info.releaseNotesEn.isEmpty()
        assert info.currentVersion != null && !info.currentVersion.isEmpty()

        assert telemetryEndpoint.checkUpdateBodies.size() == 1
        Map body = JSONObjectUtil.toObject(telemetryEndpoint.checkUpdateBodies[0], LinkedHashMap.class)
        assert body.source_id != null
        assert body.license_type != null
        assert body.version != null
        assert body.request_type == TelemetryConstant.REQUEST_TYPE_CHECK_UPDATE
        assert body.mn_ids instanceof List
        assert body.host_ids instanceof List
        assert !body.containsKey("schema_version")
        assert !body.containsKey("sourceId")

        logger.info("Test 002: check update must not write local reports/pending, expect empty dirs")
        File reports = new File(telemetryEndpoint.tempRootDir, TelemetryConstant.LOCAL_REPORTS_DIR)
        File pending = new File(telemetryEndpoint.tempRootDir, TelemetryConstant.LOCAL_PENDING_DIR)
        assert !reports.exists() || (reports.listFiles()?.length ?: 0) == 0
        assert !pending.exists() || (pending.listFiles()?.length ?: 0) == 0

        logger.info("Test 003: consent remains None after check update")
        TelemetryConsentView consent = getTelemetryConsent {} as TelemetryConsentView
        assert consent.consentGrantedAt == TelemetryConstant.CONSENT_NOT_GRANTED
    }

    void testCheckUpdateHealthFailure() {
        logger.info("Test 101: cloud health down, expect API failure TELEMETRY.4001")
        def cleaner = telemetryEndpoint.registerPostHttpHandler(
                TelemetryConstant.CLOUD_HEALTH_PATH, HttpMethod.GET,
                { TelemetryHttpClientSimulator.HttpForTest http, ErrorableValue value ->
                    return ErrorableValue.ofErrorCode(operr("health down on purpose"))
                })

        expectApiFailure({
            checkTelemetryUpdate {}
        }) {
            assert delegate.code == TelemetryErrors.TELEMETRY_CLOUD_UNREACHABLE.toString()
        }
        cleaner.getAsBoolean()
    }

    void testCheckUpdateInvalidResponse() {
        logger.info("Test 201: cloud returns invalid JSON, expect API failure TELEMETRY.4003")
        def cleaner = telemetryEndpoint.registerPostHttpHandler(
                TelemetryConstant.CLOUD_CHECK_UPDATE_PATH, HttpMethod.POST,
                { TelemetryHttpClientSimulator.HttpForTest http, ErrorableValue value ->
                    return ErrorableValue.of("not-a-json-object")
                })

        expectApiFailure({
            checkTelemetryUpdate {}
        }) {
            assert delegate.code == TelemetryErrors.TELEMETRY_CHECK_UPDATE_RESPONSE_INVALID.toString()
        }
        cleaner.getAsBoolean()
    }

    void testCheckUpdateMissingVersion() {
        logger.info("Test 301: cloud returns empty version, expect API failure TELEMETRY.4004")
        def cleaner = telemetryEndpoint.registerPostHttpHandler(
                TelemetryConstant.CLOUD_CHECK_UPDATE_PATH, HttpMethod.POST,
                { TelemetryHttpClientSimulator.HttpForTest http, ErrorableValue value ->
                    return ErrorableValue.of(JSONObjectUtil.toJsonString([
                            version         : "",
                            release_notes_zh: "zh",
                            release_notes_en: "en"
                    ]))
                })

        expectApiFailure({
            checkTelemetryUpdate {}
        }) {
            assert delegate.code == TelemetryErrors.TELEMETRY_CHECK_UPDATE_VERSION_INVALID.toString()
        }
        cleaner.getAsBoolean()
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
