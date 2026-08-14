package org.zstack.testlib.zsv.telemetry

import org.apache.commons.io.FileUtils
import org.springframework.http.HttpMethod
import org.zstack.header.errorcode.ErrorableValue
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.Spec
import org.zstack.testlib.SpecID
import org.zstack.testlib.SpecParam
import org.zstack.testlib.Test
import org.zstack.testlib.http.PostHandlerPair
import org.zstack.zsv.telemetry.client.TelemetryHttpClient
import org.zstack.zsv.telemetry.client.TelemetryLocalClient

import java.nio.file.Files
import java.util.function.BiFunction
import java.util.function.BooleanSupplier
import java.util.function.Predicate

class TelemetryVirtualEndpointSpec extends Spec {
    @SpecParam
    String endpointName = getClass().getSimpleName()
    String endpointUuid
    String tempRootDir

    final List<String> uploadedBodies = Collections.synchronizedList([])
    final List<String> checkUpdateBodies = Collections.synchronizedList([])

    TelemetryVirtualEndpointSpec(EnvSpec envSpec) {
        super(envSpec)
    }

    @Override
    SpecID create(String uuid, String sessionId) {
        tempRootDir = Files.createTempDirectory("telemetry-ut-").toAbsolutePath().toString()
        uploadedBodies.clear()
        checkUpdateBodies.clear()
        mockFactory(TelemetryHttpClient.class, { return new TelemetryHttpClientSimulator(this) })
        mockFactory(TelemetryLocalClient.class, { return new TelemetryLocalClientSimulator(this) })
        return id(endpointName, endpointUuid = uuid)
    }

    @Override
    void delete(String sessionId) {
        Test.functionForMockTestObjectFactory.remove(TelemetryHttpClient.class)
        Test.functionForMockTestObjectFactory.remove(TelemetryLocalClient.class)
        if (tempRootDir != null) {
            try {
                FileUtils.deleteDirectory(new File(tempRootDir))
            } catch (Exception ignored) {
            }
            tempRootDir = null
        }
    }

    List<PostHandlerPair<TelemetryHttpClientSimulator.HttpForTest, Object>> postHandlers = []

    BooleanSupplier registerPostHttpHandler(
            Predicate<TelemetryHttpClientSimulator.HttpForTest> predicate,
            BiFunction<TelemetryHttpClientSimulator.HttpForTest, ErrorableValue<Object>, ErrorableValue<Object>> handler) {
        def pair = new PostHandlerPair<TelemetryHttpClientSimulator.HttpForTest, Object>(
                Objects.requireNonNull(predicate),
                Objects.requireNonNull(handler))
        this.postHandlers << pair
        return { this.postHandlers.remove(pair) }
    }

    BooleanSupplier registerPostHttpHandler(
            String path,
            BiFunction<TelemetryHttpClientSimulator.HttpForTest, ErrorableValue<Object>, ErrorableValue<Object>> handler) {
        return registerPostHttpHandler({ it.path == path || it.pathWithoutIpAndPort == path }, handler)
    }

    BooleanSupplier registerPostHttpHandler(
            String path,
            HttpMethod method,
            BiFunction<TelemetryHttpClientSimulator.HttpForTest, ErrorableValue<Object>, ErrorableValue<Object>> handler) {
        return registerPostHttpHandler(
                { (it.path == path || it.pathWithoutIpAndPort == path) && it.method == method },
                handler)
    }
}
