package org.zstack.testlib

/**
 * Created by xing5 on 2017/2/22.
 */
abstract class SubCase extends Test implements Case {
    final void run() {
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
        } catch (Throwable t) {
            logger.warn("a sub case [${this.class}] fails, ${t.message}", t)
            collectErrorLog()
            throw t
        } finally {
            logger.info("start cleanup for case ${this.class}")
            try{
                long startCleanTime = System.currentTimeMillis()
                clean()
                methodsOnClean.each { it() }
                cleanEnvTime = System.currentTimeMillis() - startCleanTime
                logger.info("create env spend: ${envCreateTime}, test run spend: ${testRunTime}, clean env spend: ${cleanEnvTime}")
            }catch (Throwable t){
                collectErrorLog()
                throw t
            }
        }
    }

    @Override
    protected void configProperty() {
        configSkipMNExit()
    }

    @Override
    protected void runSubCases() {
        throw new Exception("runSubCases() cannot be called in a SubCase")
    }
}
