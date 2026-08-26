package org.zstack.testlib.vops

import org.springframework.http.HttpMethod
import org.zstack.header.errorcode.ErrorableValue
import org.zstack.externalservice.vops.VOpsClient
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.Spec
import org.zstack.testlib.SpecID
import org.zstack.testlib.SpecParam
import org.zstack.testlib.Test
import org.zstack.testlib.http.PostHandlerPair
import org.zstack.testlib.vops.VOpsClientSimulator.HttpForTest

import java.util.function.BiFunction
import java.util.function.BooleanSupplier
import java.util.function.Predicate

class VOpsVirtualEndpointSpec extends Spec {
    @SpecParam
    String endpointName = getClass().getSimpleName() // Only use for finding this spec
    String endpointUuid

    VOpsVirtualEndpointSpec(EnvSpec envSpec) {
        super(envSpec)
    }

    @Override
    SpecID create(String uuid, String sessionId) {
        mockFactory(VOpsClient.class, { return new VOpsClientSimulator(this) })
        return id(endpointName, endpointUuid = uuid)
    }

    @Override
    void delete(String sessionId) {
        Test.functionForMockTestObjectFactory.remove(VOpsClient.class)
    }

    public List<PostHandlerPair<HttpForTest, Object>> postHandlers = []

    /**
     * @return a function to remove this handler
     */
    BooleanSupplier registerPostHttpHandler(
            Predicate<HttpForTest> predicate,
            BiFunction<HttpForTest, ErrorableValue<Object>, ErrorableValue<Object>> handler) {
        def pair = new PostHandlerPair<HttpForTest, Object>(
                Objects.requireNonNull(predicate),
                Objects.requireNonNull(handler))

        this.postHandlers << pair
        return { this.postHandlers.remove(pair) }
    }

    /**
     * @return a function to remove this handler
     */
    BooleanSupplier registerPostHttpHandler(
            String path,
            BiFunction<HttpForTest, ErrorableValue<Object>, ErrorableValue<Object>> handler) {
        return registerPostHttpHandler({ it.path == path || it.pathWithoutIpAndPort == path }, handler)
    }

    /**
     * @return a function to remove this handler
     */
    BooleanSupplier registerPostHttpHandler(
            String path,
            HttpMethod method,
            BiFunction<HttpForTest, ErrorableValue<Object>, ErrorableValue<Object>> handler) {
        return registerPostHttpHandler(
            {
                (it.path == path || it.pathWithoutIpAndPort == path) && it.method == method
            }, handler)
    }
}
