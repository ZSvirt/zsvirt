package org.zstack.testlib

import org.zstack.utils.ShellUtils

/**
 * Created by lining on 2017/7/12.
 */
abstract class StabilityTest extends Test implements Case{


    private List<Case> targetSubCaseList

    SpringSpec getDefaultSpringSpec(){
        throw StopTestSuiteException()
    }

    String getFailureLogsDirName() {
        throw StopTestSuiteException()
    }

    @Override
    void setup() {
        String targetCaseList = System.getProperty(targetSubCaseParamKey)
        List<Case> caseList = buildTargetCaseList(targetCaseList)

        assert !checkSubCaseInclusionItself(caseList)

        if(caseList.size() == 1){
            Case subCase = caseList[0]
            subCase.setup()
            SpringSpec subCaseSpringSpec = subCase._springSpec
            useSpring(subCaseSpringSpec)
        } else {
            useSpring(getDefaultSpringSpec())
        }
    }

    @Override
    void environment() {
        return
    }

    @Override
    void run() {
        envCreateTime = 0
        testRunTime = 0
        cleanEnvTime = 0
        try {
            long startEnvCreateTime = System.currentTimeMillis()
            environment()
            long endEnvCreateTime = System.currentTimeMillis()
            envCreateTime += System.currentTimeMillis() - startEnvCreateTime
            test()
            testRunTime += System.currentTimeMillis() - startEnvCreateTime - envCreateTime

            logger.info("start cleanup for case ${this.class}")
            long startCleanTime = System.currentTimeMillis()
            clean()
            methodsOnClean.each { it() }
            cleanEnvTime = System.currentTimeMillis() - startCleanTime
            logger.info("create env spend: ${envCreateTime}, test run spend: ${testRunTime}, clean env spend: ${cleanEnvTime}")
        } catch (Throwable t) {
            logger.warn("a sub case [${this.class}] fails, ${t.message}", t)
            throw t
        }
    }

    @Override
    void test() {
        String targetCaseList = System.getProperty(targetSubCaseParamKey)
        String times = System.getProperty(subCaseExecutionTimesKey)

        logger.info("start stability test, targetCaseList = ${targetCaseList}, times = ${times != null ? times : 1}")

        if(times == null){
            runTestWithTimes(targetCaseList)
        }else{
            runTestWithTimes(targetCaseList, Integer.parseInt(times))
        }

        logger.info("stability test finished")
    }

    private void runTestWithTimes(String targetCaseList, int times = 1){
        assert times >= 1

        targetCaseList.split(",").each { cname ->
            cname = cname.trim()

            int index = 0
            while (index < times){
                // create a new case for every run, otherwise
                // the stateful cases will fail
                Case subCase = buildCase(cname)
                if (subCase.class.isAnnotationPresent(SkipTestSuite.class)) {
                    break
                }

                index ++

                logger.info("stability test, a sub case [${subCase.class}] start running, current execution times is ${index}")
                long startTime = new Date().getTime()
                try{
                    subCase.run()
                }catch (Throwable t){
                    logger.error("stability test fails, a sub case [${subCase.class}] fails, current execution times is ${index}, ${t.message}" ,t)
                    throw t
                } finally {
                    assert currentEnvSpec == null: "EnvSpec is not cleaned after execute ${this.class}."
                }
                long spendTime = (new Date().getTime() - startTime) / 1000
                logger.info("stability test, a sub case [${subCase.class}] test pass, current execution times is ${index}, spend time is ${spendTime} secs")

                assert index > 0 && index <= times
            }
        }
    }

    protected Case buildCase(String caseName) {
        Case subCase = Class.forName(caseName).newInstance() as Case
        def resultDir = [getResultDirBase(), getFailureLogsDirName()].join("/")
        def dir = new File(resultDir)
        dir.deleteDir()
        dir.mkdirs()

        String caseLogStartLine = "stability test, a sub case \\[class ${caseName}\\] start running"

        subCase.metaClass.collectErrorLog = {
            File failureLogDir = new File([dir.absolutePath, "failureLogs", caseName.replace(".", "_")].join("/"))
            failureLogDir.mkdirs()
            File failureLog = new File([failureLogDir.absolutePath, "case.log"].join("/"))

            File mgmtLogPath = new File([System.getProperty("user.dir"), "management-server.log"].join("/"))

            ShellUtils.run("""\
start=`grep -nr "$caseLogStartLine" ${mgmtLogPath.absolutePath} | grep -v ShellUtils | tail -n 1 | gawk '{print \$1}' FS=":"`
tail -n +\$start ${mgmtLogPath.absolutePath} > ${failureLog.absolutePath}

mysqldump -u root zstack > ${failureLogDir.absolutePath}/dbdump.sql
""", false)
        }

        return subCase
    }

    private List<Case> buildTargetCaseList(String targetCaseList){
        if(this.targetSubCaseList != null){
            return this.targetSubCaseList
        }

        List<Case> result = []

        if(targetCaseList == null || targetCaseList.isEmpty()){
            return result
        }

        try {
            String[] caseClassNameList = targetCaseList.split(",")
            for(String caseClassName : caseClassNameList){
                result.add(buildCase(caseClassName))
            }
        }catch (Exception e){
            logger.error("build target subCase error occurred", e)
            throw e
        }
        this.targetSubCaseList = result

        String caseNames = result.collect {it.class.name}.join(", ")
        logger.debug("stability test target subcase list : ${caseNames}")

        return result
    }

    private boolean checkSubCaseInclusionItself(List<Case> cases){
        boolean result = false

        cases.each {
            if(it.class.name.equals(this.class.name)){
                result = true
            }
        }

        return result
    }

    @Override
    void clean() {
        return
    }
}
