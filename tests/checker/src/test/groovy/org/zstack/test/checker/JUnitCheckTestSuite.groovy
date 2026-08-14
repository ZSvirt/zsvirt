package org.zstack.test.checker

import org.junit.runner.JUnitCore
import org.junit.runner.Result
import org.junit.runner.RunWith
import org.junit.runner.notification.Failure
import org.junit.runners.Suite
import org.zstack.core.Platform
import org.zstack.test.checker.audits.AuditChecker
import org.zstack.test.checker.code.LFFileChecker
import org.zstack.test.checker.route.ApiServiceIdRegistryDump

@RunWith(Suite.class)
@Suite.SuiteClasses([
    AuditChecker.class,
    LFFileChecker.class,
    ApiServiceIdRegistryDump.class,
])
class JUnitCheckTestSuite {
    static void runAllTestCases() {
        // Avoid System.exit on Platform boot failure so Surefire can report the real error
        System.setProperty("exitJVMOnBootFailure", "false")
        // Trigger Platform static init (zstack.properties / global property / @StaticInit)
        Platform.getManagementServerId()

        Result result = JUnitCore.runClasses(JUnitCheckTestSuite.class)

        List<Failure> failures = result.getFailures()
        if (!failures.isEmpty()) {
            List<String> errors = failures.collect { it.toString() }
            assert false : "JUnit test fail, " +  errors.toString()
        }
    }
}
