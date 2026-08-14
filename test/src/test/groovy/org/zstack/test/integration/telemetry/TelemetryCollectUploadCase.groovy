package org.zstack.test.integration.telemetry

import org.springframework.http.HttpMethod
import org.zstack.core.Platform
import org.zstack.core.cloudbus.CloudBus
import org.zstack.header.errorcode.ErrorableValue
import org.zstack.header.message.MessageReply
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.zsv.telemetry.TelemetryHttpClientSimulator
import org.zstack.testlib.zsv.telemetry.TelemetryVirtualEndpointSpec
import org.zstack.utils.gson.JSONObjectUtil
import org.zstack.zsv.telemetry.TelemetryConstant
import org.zstack.zsv.telemetry.client.TelemetryLocalClient
import org.zstack.zsv.telemetry.header.TelemetryGlobalConfig
import org.zstack.zsv.telemetry.header.TelemetryRunCollectMsg
import org.zstack.zsv.telemetry.header.TelemetryRunUploadMsg

import java.nio.charset.StandardCharsets

import static org.zstack.core.Platform.operr

class TelemetryCollectUploadCase extends SubCase {
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
            testCollectWithoutConsent()
            testCollectAndUpload()
            testUploadHealthFailureKeepsPending()
            testArchivedReportNotRequeuedToPending()
            testCapacityKeepsLatestReport()
        }
    }

    void prepare() {
        telemetryEndpoint = env.children.find { it instanceof TelemetryVirtualEndpointSpec } as TelemetryVirtualEndpointSpec
        bus = bean(CloudBus.class)
        assert telemetryEndpoint.tempRootDir != null
    }

    void testCollectWithoutConsent() {
        logger.info("Test 001: collect without consent, expect no local files")
        MessageReply reply = callLocal(new TelemetryRunCollectMsg())
        assert reply.success

        File reports = new File(telemetryEndpoint.tempRootDir, TelemetryConstant.LOCAL_REPORTS_DIR)
        File pending = new File(telemetryEndpoint.tempRootDir, TelemetryConstant.LOCAL_PENDING_DIR)
        assert !reports.exists() || (reports.listFiles()?.length ?: 0) == 0
        assert !pending.exists() || (pending.listFiles()?.length ?: 0) == 0
    }

    void testCollectAndUpload() {
        logger.info("Test 101: enable consent then collect with health down, expect nested snake_case JSON and pending")
        updateTelemetryConsent {
            delegate.action = TelemetryConstant.CONSENT_ACTION_ENABLED
            delegate.agreedToTerms = true
        }

        def healthDown = telemetryEndpoint.registerPostHttpHandler(
                TelemetryConstant.CLOUD_HEALTH_PATH, HttpMethod.GET,
                { TelemetryHttpClientSimulator.HttpForTest http, ErrorableValue value ->
                    return ErrorableValue.ofErrorCode(operr("hold pending for assert"))
                })

        try {
            MessageReply collectReply = callLocal(new TelemetryRunCollectMsg())
            assert collectReply.success

            File reportsDir = new File(telemetryEndpoint.tempRootDir, TelemetryConstant.LOCAL_REPORTS_DIR)
            File[] reportFiles = reportsDir.listFiles({ dir, name -> name.endsWith(".json") } as FilenameFilter)
            assert reportFiles != null && reportFiles.length == 1

            Map report = JSONObjectUtil.toObject(reportFiles[0].getText(StandardCharsets.UTF_8.name()), LinkedHashMap.class)
            assert report.containsKey("schema_version")
            assert report.containsKey("snapshot_date")
            assert report.containsKey("source_id")
            assert report.containsKey("system")
            assert report.containsKey("compute")
            assert report.containsKey("storage")
            assert ((Map) report.system).containsKey("license_type")
            assert ((Map) report.compute).containsKey("mn_ids")
            assert ((Map) report.compute).containsKey("host_ids")
            assert ((Map) report.compute).containsKey("vm_os_distribution")
            assert !report.containsKey("schemaVersion")

            File pendingDir = new File(telemetryEndpoint.tempRootDir, TelemetryConstant.LOCAL_PENDING_DIR)
            File[] pendingFiles = pendingDir.listFiles({ dir, name -> name.endsWith(".json") } as FilenameFilter)
            assert pendingFiles != null && pendingFiles.length >= 1
        } finally {
            healthDown.getAsBoolean()
        }

        logger.info("Test 101b: collect must not upload by itself even when cloud health is up")
        telemetryEndpoint.uploadedBodies.clear()
        assert callLocal(new TelemetryRunCollectMsg()).success
        assert telemetryEndpoint.uploadedBodies.isEmpty()
        File pendingAfterCollect = new File(telemetryEndpoint.tempRootDir, TelemetryConstant.LOCAL_PENDING_DIR)
        assert (pendingAfterCollect.listFiles({ dir, name -> name.endsWith(".json") } as FilenameFilter)?.length ?: 0) >= 1

        logger.info("Test 102: upload pending via upload task, expect cloud POST and pending cleared")
        telemetryEndpoint.uploadedBodies.clear()
        MessageReply uploadReply = callLocal(new TelemetryRunUploadMsg())
        assert uploadReply.success
        assert telemetryEndpoint.uploadedBodies.size() >= 1

        File pendingDir = new File(telemetryEndpoint.tempRootDir, TelemetryConstant.LOCAL_PENDING_DIR)
        File[] pendingFiles = pendingDir.listFiles({ dir, name -> name.endsWith(".json") } as FilenameFilter)
        assert pendingFiles == null || pendingFiles.length == 0
    }

    void testUploadHealthFailureKeepsPending() {
        logger.info("Test 201: collect with health down then upload still keeps pending")
        def cleaner = telemetryEndpoint.registerPostHttpHandler(
                TelemetryConstant.CLOUD_HEALTH_PATH, HttpMethod.GET,
                { TelemetryHttpClientSimulator.HttpForTest http, ErrorableValue value ->
                    return ErrorableValue.ofErrorCode(operr("health down on purpose"))
                })

        try {
            assert callLocal(new TelemetryRunCollectMsg()).success

            File pendingDir = new File(telemetryEndpoint.tempRootDir, TelemetryConstant.LOCAL_PENDING_DIR)
            int before = pendingDir.listFiles({ dir, name -> name.endsWith(".json") } as FilenameFilter)?.length ?: 0
            assert before >= 1

            assert callLocal(new TelemetryRunUploadMsg()).success
            int after = pendingDir.listFiles({ dir, name -> name.endsWith(".json") } as FilenameFilter)?.length ?: 0
            assert after >= before
        } finally {
            cleaner.getAsBoolean()
        }
    }

    void testArchivedReportNotRequeuedToPending() {
        logger.info("Test 202: already-uploaded report in reports/ must not re-enter pending on next collect")
        // Simulate leftover local copy after successful upload (pending cleared, reports kept).
        TelemetryLocalClient localClient = Platform.New({ new TelemetryLocalClient() }) as TelemetryLocalClient
        File reportsDir = new File(localClient.reportsDir())
        reportsDir.mkdirs()
        File archived = new File(reportsDir, "daily-report-2020-01-01.json")
        archived.write('{"schema_version":"1.0","snapshot_date":"2020-01-01"}', StandardCharsets.UTF_8.name())

        def healthDown = telemetryEndpoint.registerPostHttpHandler(
                TelemetryConstant.CLOUD_HEALTH_PATH, HttpMethod.GET,
                { TelemetryHttpClientSimulator.HttpForTest http, ErrorableValue value ->
                    return ErrorableValue.ofErrorCode(operr("hold pending to inspect queue"))
                })

        try {
            assert callLocal(new TelemetryRunCollectMsg()).success

            File[] reportFiles = reportsDir.listFiles({ dir, name -> name.endsWith(".json") } as FilenameFilter)
            assert reportFiles != null && reportFiles.length == 1
            assert !reportFiles[0].getName().contains("2020-01-01")
            assert !archived.exists()

            File pendingDir = new File(localClient.pendingDir())
            File[] pendingFiles = pendingDir.listFiles({ dir, name -> name.endsWith(".json") } as FilenameFilter)
            assert pendingFiles != null
            assert pendingFiles.every { !it.getName().contains("2020-01-01") }
        } finally {
            healthDown.getAsBoolean()
        }
    }

    void testCapacityKeepsLatestReport() {
        logger.info("Test 301: capacity eviction keeps one latest report")
        updateGlobalConfig {
            delegate.category = TelemetryGlobalConfig.CATEGORY
            delegate.name = TelemetryGlobalConfig.LOCAL_MAX_BYTES.name
            delegate.value = "2048"
        }

        try {
            TelemetryLocalClient localClient = Platform.New({ new TelemetryLocalClient() }) as TelemetryLocalClient
            File pendingDir = new File(localClient.pendingDir())
            pendingDir.mkdirs()
            1.upto(5) { i ->
                File f = new File(pendingDir, "pad-${i}.json")
                f.write("x" * 800, StandardCharsets.UTF_8.name())
            }

            assert callLocal(new TelemetryRunCollectMsg()).success

            File reportsDir = new File(localClient.reportsDir())
            File[] reportFiles = reportsDir.listFiles({ dir, name -> name.endsWith(".json") } as FilenameFilter)
            assert reportFiles != null && reportFiles.length == 1
        } finally {
            updateGlobalConfig {
                delegate.category = TelemetryGlobalConfig.CATEGORY
                delegate.name = TelemetryGlobalConfig.LOCAL_MAX_BYTES.name
                delegate.value = "52428800"
            }
            updateTelemetryConsent {
                delegate.action = TelemetryConstant.CONSENT_ACTION_DISABLED
            }
        }
    }

    private MessageReply callLocal(Object msg) {
        bus.makeLocalServiceId(msg, TelemetryConstant.SERVICE_ID)
        return bus.call(msg) as MessageReply
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
