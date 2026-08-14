package org.zstack.test.integration.rest

import org.zstack.rest.TypeVerifier
import org.zstack.test.integration.ZStackTest
import org.zstack.testlib.SubCase

class TypeVerifierCase extends SubCase {
    @Override
    void clean() {

    }

    @Override
    void setup() {
        useSpring(ZStackTest.springSpec)
    }

    @Override
    void environment() {

    }

    @Override
    void test() {
        String result = TypeVerifier.verify(Api.class.getDeclaredField("number"), "1.2")
        assert result != null
        logger.debug(result)
        String result2 = TypeVerifier.verify(Api.class.getDeclaredField("number"), "1")
        assert result2 == null

        String result3 = TypeVerifier.verify(Api.class.getDeclaredField("condition"), "tru")
        assert result3 != null
        logger.debug(result3)
        String result4 = TypeVerifier.verify(Api.class.getDeclaredField("condition"), "true")
        assert result4 == null

        String result5 = TypeVerifier.verify(Api.class.getDeclaredField("longNumber"), "201804031345.1")
        assert result5 != null
        logger.debug(result5)
        String result6 = TypeVerifier.verify(Api.class.getDeclaredField("longNumber"), "201804031345")
        assert result6 == null
    }

    class Api {
        private int number
        private boolean condition
        private long longNumber
    }
}
