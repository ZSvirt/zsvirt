package org.zstack.test.integration.core

import org.zstack.core.Platform
import org.zstack.core.step.StepRun
import org.zstack.core.step.StepRunCondition
import org.zstack.header.core.Completion
import org.zstack.header.errorcode.ErrorCode
import org.zstack.testlib.SubCase
/**
 * Created by mingjian.deng on 2019/1/30.*/
class StepRunCase extends SubCase {
    def testElements = new ArrayList()

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
        prepare()
        testRetry1()
    }

    void prepare() {
        1.upto(10) {
            testElements.add(Platform.uuid)
        }
        Collections.sort(testElements)
    }

    void testRetry1() {
        def elements = new ArrayList()
        def times = new ArrayList()
        def flag = "begin"

        new StepRun<String>(testElements) {
            @Override
            @StepRunCondition(stepLimit = 3)
            protected void call(List<String> stepElements, Completion completion) {
                times.add(stepElements.size())
                elements.addAll(stepElements)
                completion.success()
            }
        }.run(new Completion(null) {
            @Override
            void success() {
                flag = "success"
            }

            @Override
            void fail(ErrorCode errorCode) {
                flag = "failed"
            }
        })

        retryInSecs {
            flag == "success"
        }

        Collections.sort(elements)
        assert elements == testElements
        assert times[0] == 3
        assert times[1] == 3
        assert times[2] == 3
        assert times[3] == 1
    }
}
