package org.zstack.testlib.collectstrategy

import org.zstack.core.Platform
import org.zstack.testlib.Case
import org.zstack.testlib.Test
import org.zstack.utils.Utils
import org.zstack.utils.gson.JSONObjectUtil
import org.zstack.utils.logging.CLogger

import java.lang.reflect.Modifier

/**
 * Created by lining on 2017/7/19.
 */

/*
Example:

 primary storage test package
├── local
│   ├── localStorageCapacityCase
│   └── localStorageStateCase
│
├── nfs
│   ├── NfsCapacityCase
│   ├── NfsCapacityStateCase
│   └── NfsTest
│
├── PrimaryStorageCapacityCase
├── PrimaryStorageStateCase
└── PrimaryStorageTest

 PrimaryStorageTest subCases is [PrimaryStorageCapacityCase, PrimaryStorageStateCase, localStorageCapacityCase,localStorageStateCase]

 */
class NearestSubCaseCollectionStrategy implements SubCaseCollectionStrategy{

    final CLogger logger = Utils.getLogger(this.getClass())

    final static String strategyName = "Nearest"

    @Override
    List<Class> collectSubCases(Test test) {
        assert null != test : "test is null, can not find any subcase"

        // collect testsuite
        def testSuites = Platform.reflections.getSubTypesOf(Test.class)
        testSuites = testSuites.findAll{ it.package != null && it.package.name.startsWith(test.class.package.name) && it.name.endsWith("Test") }
        testSuites = testSuites.sort{ a, b ->
            return a.name.compareTo(b.name)
        }
        assert 1 == testSuites.findAll{ it.name == test.class.name}.size() : "Can not find current testsuite class"

        // collect case
        def cases = Platform.reflections.getSubTypesOf(Case.class).findAll { !Modifier.isAbstract(it.modifiers) }
        cases = cases.findAll { it.package.name.startsWith(test.class.package.name) }
        cases = cases.sort{ a, b ->
            return a.name.compareTo(b.name)
        }

        Map<String, List<Class>> testSuiteSubCasesContainer = buildTestSuiteSubCaseContainer(testSuites, cases)
        printLog(testSuiteSubCasesContainer)

        return testSuiteSubCasesContainer.get(test.class.name)
    }

    private Map<String, List<Class>> buildTestSuiteSubCaseContainer(List<Class> testSuites, List<Class> cases){

        Map<String, List<Class>> testSuiteSubCasesContainer = new HashMap<>()
        for(Class testClass : testSuites){
            List<Class> list = new ArrayList<>()
            testSuiteSubCasesContainer.put(testClass.name, list)
        }

        // find the nearest testsuite for the subcase
        for(Class subCaseClass : cases){
            int packageLayer = -1
            Class nearestTestSuite = null

            for(Class testSuiteClass : testSuites){
                String subCasePackage = subCaseClass.package.name
                String testPackage = testSuiteClass.package.name

                if(!subCasePackage.startsWith(testPackage)){
                    continue
                }

                if (!subCasePackage.replace(testPackage, "").startsWith(".") && !subCasePackage.equals(testPackage)) {
                    logger.debug("${subCasePackage}, ${testPackage} skiped".toString())
                    // NOTE(weiw): like org.zstack.storage vs org.zstack.storageDevice, if case is org.zstack.storageDevice.AbcCase,
                    // the first should not be match
                    continue
                }

                String defferPackage = subCasePackage.replace(testPackage, "")
                int layer = defferPackage.length() - defferPackage.replaceAll("\\.", "").length()
                if(packageLayer == -1 || packageLayer >  layer){
                    packageLayer =  layer
                    nearestTestSuite = testSuiteClass
                }else if(packageLayer ==  layer){
                    assert false : "Multiple testsuite are not allowed under a package path"
                }
            }

            assert packageLayer != -1 && nearestTestSuite != null
            testSuiteSubCasesContainer.get(nearestTestSuite.name).add(subCaseClass)
        }

        int caseCount = 0
        testSuiteSubCasesContainer.each{ key,value ->
            caseCount += value.size()
        }
        assert cases.size() == caseCount : "collect subCases error"

        return testSuiteSubCasesContainer
    }

    private void printLog(Map<String, List<Class>> testSuiteSubCaseMap){
        Map<String, List<String>> nameMap = new HashMap<>()

        testSuiteSubCaseMap.each { key, value ->
            List<String> caseNames = []
            value.forEach{
                caseNames.add(it.name)
            }
            nameMap.put(key, caseNames)
        }
        String nameMapString = JSONObjectUtil.toJsonString(nameMap)
        logger.info("TestSuite-SubCase map : " + nameMapString)
    }

}
