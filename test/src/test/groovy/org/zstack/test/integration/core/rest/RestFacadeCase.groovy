package org.zstack.test.integration.core.rest

import org.springframework.http.HttpEntity
import org.zstack.core.rest.RESTFacadeImpl
import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.errorcode.SysErrors
import org.zstack.header.rest.AsyncRESTCallback
import org.zstack.test.integration.ZStackTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.WebBeanConstructor
import org.zstack.utils.URLBuilder

import java.util.concurrent.TimeUnit

class RestFacadeCase extends SubCase {
    EnvSpec env
    String BASE_URL = "/test-rest-facade"

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(ZStackTest.springSpec)
    }

    @Override
    void environment() {
        env = env {

        }
    }

    @Override
    void test() {
        env.create {
            testRestFacadeFailureWillBeRecorded()
        }
    }

    void testRestFacadeFailureWillBeRecorded() {
        RESTFacadeImpl restf = bean(RESTFacadeImpl.class)

        String requestContent = "test"
        boolean hangUntilTimeout = true
        env.simulator(BASE_URL) { HttpEntity<String> e ->
            while (hangUntilTimeout) {
                sleep(100)
            }

            assert e.body == requestContent
            return e.toString()
        }

        boolean timeout = false
        boolean success = false
        String url = URLBuilder.buildHttpUrl("127.0.0.1", WebBeanConstructor.port, BASE_URL)
        restf.asyncJsonPost(url, requestContent, new AsyncRESTCallback(null) {
            @Override
            void fail(ErrorCode err) {
                timeout = err.isError(SysErrors.TIMEOUT)
            }

            @Override
            void success(HttpEntity<String> responseEntity) {
                success = true
            }
        }, TimeUnit.MILLISECONDS, 200)

        retryInSecs {
            assert timeout
            assert !success
        }

        hangUntilTimeout = false
        retryInSecs {
            assert restf.notifiedFailureHttpTasks.size() == 1
        }

        restf.notifiedFailureHttpTasks.clear()
    }
}
