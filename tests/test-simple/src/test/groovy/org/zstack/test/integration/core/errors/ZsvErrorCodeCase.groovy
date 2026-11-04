package org.zstack.test.integration.core.errors

import org.zstack.core.Platform
import org.zstack.testlib.SubCase

class ZsvErrorCodeCase extends SubCase {
    @Override
    void clean() {
    }

    @Override
    void setup() {
    }

    @Override
    void environment() {
    }

    @Override
    void test() {
        testPlatformError()
    }

    static void testPlatformError() {
        def error1 = Platform.operr("error1")
        error1.i18nDetails = "错误1中文"

        def error2 = Platform.operr("error2: uuid=%s: %s", "657e44a522ee42b89b306bbf661c1885", error1)
        assert error2.details == "error2: uuid=657e44a522ee42b89b306bbf661c1885: error1"
        assert error2.i18nDetails == "error2: uuid=657e44a522ee42b89b306bbf661c1885: 错误1中文"
    }
}
