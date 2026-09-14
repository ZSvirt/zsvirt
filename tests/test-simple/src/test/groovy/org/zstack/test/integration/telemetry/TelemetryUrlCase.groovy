package org.zstack.test.integration.telemetry

import org.springframework.http.HttpMethod
import org.zstack.core.cloudbus.CloudBus
import org.zstack.header.errorcode.ErrorableValue
import org.zstack.header.message.MessageReply
import org.zstack.sdk.GlobalConfigInventory
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.zsv.telemetry.TelemetryHttpClientSimulator
import org.zstack.testlib.zsv.telemetry.TelemetryVirtualEndpointSpec
import org.zstack.zsv.telemetry.TelemetryConstant
import org.zstack.zsv.telemetry.header.TelemetryGlobalConfig
import org.zstack.zsv.telemetry.header.TelemetryRunCollectMsg
import org.zstack.zsv.telemetry.header.TelemetryRunUploadMsg

class TelemetryUrlCase extends SubCase {
    EnvSpec env
    TelemetryVirtualEndpointSpec telemetryEndpoint
    CloudBus bus

    @Override
    void setup() {
        useSpring(TelemetryTest.springSpec)
    }

    @Override
    void environment() {
        env = makeEnv {
            telemetryEndpoint {
                endpointName = "telemetry-endpoint"
            }
        }
    }

    @Override
    void test() {
        env.create {
            prepare()
            testTelemetryUrl()
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

        telemetryEndpoint = env.children.find { it instanceof TelemetryVirtualEndpointSpec } as TelemetryVirtualEndpointSpec
        bus = bean(CloudBus.class)
    }

    void testTelemetryUrl() {
        logger.info("Test 001: collect")

        updateTelemetryConsent {
            delegate.action = TelemetryConstant.CONSENT_ACTION_ENABLED
            delegate.agreedToTerms = true
        }

        def msg001a = new TelemetryRunCollectMsg()
        bus.makeLocalServiceId(msg001a, TelemetryConstant.SERVICE_ID)
        def reply001a = bus.call(msg001a) as MessageReply
        assert reply001a.success

        File reportsDir = new File(telemetryEndpoint.tempRootDir, TelemetryConstant.LOCAL_REPORTS_DIR)
        File[] reportFiles = reportsDir.listFiles({ dir, name -> name.endsWith(".json") } as FilenameFilter)
        assert reportFiles != null && reportFiles.length == 1

        logger.info("Test 002: cloud.base.url=None, check upload url")
        def configs002 = queryGlobalConfig {
            delegate.conditions = [
                "category=${TelemetryGlobalConfig.CLOUD_BASE_URL.category}".toString(),
                "name=${TelemetryGlobalConfig.CLOUD_BASE_URL.name}".toString(),
            ]
        } as List<GlobalConfigInventory>
        assert configs002.size() == 1
        assert configs002[0].value == "None"

        def httpList002 = [] as List<TelemetryHttpClientSimulator.HttpForTest>
        def cleaner002a = telemetryEndpoint.registerPostHttpHandler(
                TelemetryConstant.CLOUD_HEALTH_PATH, HttpMethod.GET,
                { TelemetryHttpClientSimulator.HttpForTest http, ErrorableValue value ->
                    httpList002.add(http)
                    return value
                })
        def cleaner002b = telemetryEndpoint.registerPostHttpHandler(
                TelemetryConstant.CLOUD_REPORTS_PATH, HttpMethod.POST,
                { TelemetryHttpClientSimulator.HttpForTest http, ErrorableValue value ->
                    httpList002.add(http)
                    return value
                })

        def msg002a = new TelemetryRunUploadMsg()
        bus.makeLocalServiceId(msg002a, TelemetryConstant.SERVICE_ID)
        def reply002a = bus.call(msg002a) as MessageReply
        assert reply002a.success

        logger.info("health https size = " + httpList002.size())
        httpList002.forEach {
            logger.info(it.path)
        }

        assert httpList002.size() >= 1
        assert httpList002.every { it.path.contains(":" + TelemetryConstant.MOCK_CLOUD_BASE_PORT) }

        logger.info("Test 009: clean")
        cleaner002a.getAsBoolean()
        cleaner002b.getAsBoolean()
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
