package org.zstack.test.integration.zql

import org.zstack.sdk.BatchQueryAction
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.test.integration.storage.Env
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.Test

class BatchQueryCase extends SubCase{
    EnvSpec env

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.localStorageOneVmEnv()
    }

    @Override
    void test() {
        env.create {
            testRemoteCodeExecution()
        }
    }

    void testRemoteCodeExecution(){
        String queryScript = '''@groovy.transform.ASTTest(value={assert java.lang.Runtime.getRuntime().exec("touch /tmp/pwned")}) def x'''

        BatchQueryAction action = new BatchQueryAction(
                sessionId: Test.currentEnvSpec.session.uuid,
                script: queryScript
        )
        BatchQueryAction.Result result = action.call()
        assert result.error != null
        result.error.details.contains("Annotation groovy.transform.ASTTest cannot be used in the sandbox")

        queryScript = '''
import groovy.transform.*
@ASTTest(value={assert java.lang.Runtime.getRuntime().exec("touch /tmp/pwned")}) def x
'''
        action = new BatchQueryAction(
                sessionId: Test.currentEnvSpec.session.uuid,
                script: queryScript
        )
        result = action.call()
        result.error.details.contains("Annotation groovy.transform.ASTTest cannot be used in the sandbox")

    }


    @Override
    void clean() {
        env.delete()
    }

}
