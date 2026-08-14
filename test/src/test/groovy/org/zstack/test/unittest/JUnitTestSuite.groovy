package org.zstack.test.unittest

import org.junit.runner.JUnitCore
import org.junit.runner.Result
import org.junit.runner.RunWith
import org.junit.runner.notification.Failure
import org.junit.runners.Suite
import org.zstack.configuration.OfferingUserConfigUtils
import org.zstack.test.unittest.utils.NetworkUtilsCase
import org.zstack.test.unittest.utils.OfferingUserConfigUtilsCase
import org.zstack.test.unittest.utils.ShellEscapeCase
import org.zstack.test.unittest.utils.SizeUnitUtilsCase

import java.util.stream.Collectors

/**
 * Created by lining on 2018/3/18.
 */

@RunWith(Suite.class)
@Suite.SuiteClasses([
        NetworkUtilsCase.class,
        OfferingUserConfigUtilsCase.class,
        ShellEscapeCase.class,
        SizeUnitUtilsCase.class
    ])
class JUnitTestSuite {

    static void runAllTestCases() {
        Result result = JUnitCore.runClasses(JUnitTestSuite.class)

        List<Failure> failures = result.getFailures()
        if (!failures.isEmpty()) {
            List<String> errors = failures.stream().map{failure -> failure.toString()}.collect(Collectors.toList())
            assert false : "JUnit test fail, " +  errors.toString()
        }
    }
}
