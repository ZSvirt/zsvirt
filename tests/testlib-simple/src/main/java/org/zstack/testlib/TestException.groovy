package org.zstack.testlib

/**
 * Created by xing5 on 2017/2/12.
 */
class TestException extends RuntimeException {
    TestException() {
    }

    TestException(String var1) {
        super(var1)
    }

    TestException(String var1, Throwable var2) {
        super(var1, var2)
    }

    TestException(Throwable var1) {
        super(var1)
    }

    TestException(String var1, Throwable var2, boolean var3, boolean var4) {
        super(var1, var2, var3, var4)
    }
}
