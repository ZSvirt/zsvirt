package org.zstack.test.integration.core

import org.zstack.core.Platform
import org.zstack.test.integration.stabilisation.StabilityTestCase
import org.zstack.testlib.SubCase
import org.zstack.testlib.Test

import java.lang.reflect.Modifier

/**
 * Created by lining on 2017/7/21.
 */
class CaseNameCheckCase extends SubCase {

    private static final List<String> ignoreCheckListForTestSuite = [StabilityTestCase.class.name]

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
        checkTestSuiteName()
        checkTestCaseName()
    }

    void checkTestSuiteName(){
        List<String> invalidNameList = []

        def tests = Platform.reflections.getSubTypesOf(Test.class).findAll { !Modifier.isAbstract(it.modifiers) }
        tests.forEach{ it ->
            Class caseClass = it
            boolean result = it.package != null && !it.name.endsWith("Test") && !SubCase.isAssignableFrom(it) && ignoreCheckListForTestSuite.find{ caseClass.name == it } == null

            if(result){
                invalidNameList.add(it.name)
            }
        }

        assert 0 == invalidNameList.size() : "invalid testsuite name, testsuite name must end with 'Test', ${invalidNameList}"
    }

    void checkTestCaseName(){
        List<String> invalidNameList = []

        def tests = Platform.reflections.getSubTypesOf(SubCase.class).findAll { !Modifier.isAbstract(it.modifiers) }
        tests.forEach{ it ->
            boolean result = it.package != null && !it.name.endsWith("Case")

            if(result){
                invalidNameList.add(it.name)
            }
        }

        assert 0 == invalidNameList.size() : "invalid SubCase name, testsuite name must end with 'Case', ${invalidNameList}"
    }

}