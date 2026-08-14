package org.zstack.testlib

/**
 * Created by xing5 on 2017/3/16.
 */
class StopTestSuiteException extends Exception {
    StopTestSuiteException() {
    }

    StopTestSuiteException(String var1) {
        super(var1)
    }

    StopTestSuiteException(String var1, Throwable var2) {
        super(var1, var2)
    }

    StopTestSuiteException(Throwable var1) {
        super(var1)
    }

    StopTestSuiteException(String var1, Throwable var2, boolean var3, boolean var4) {
        super(var1, var2, var3, var4)
    }
}
