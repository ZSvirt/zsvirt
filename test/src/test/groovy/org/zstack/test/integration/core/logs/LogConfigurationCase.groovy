package org.zstack.test.integration.core.logs

import org.zstack.core.log.Log4jXMLModifier
import org.zstack.core.log.LogConstant
import org.zstack.core.log.LogGlobalConfig
import org.zstack.core.log.LogGlobalProperty
import org.zstack.core.log.LogManagerImpl
import org.zstack.test.integration.ZStackTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

class LogConfigurationCase extends SubCase {
    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(ZStackTest.springSpec)
    }

    @Override
    void environment() {
        env = env {
        }
    }

    @Override
    void test() {
        env.create {
            prepare()
            testUpdateLogConfig()
        }
    }

    void prepare() {
        assert bean(LogManagerImpl)
    }

    void testUpdateLogConfig() {
        expectApiFailure({
            updateGlobalConfig {
                category = LogGlobalConfig.CATEGORY
                name = LogGlobalConfig.LOG_DELETE_ACCUMULATED_FILE_SIZE.getName()
                value = ((Integer.MAX_VALUE as long) + 1L).toString()
            }
        }) {
            assert delegate.code == "SYS.1007"
        }

        expectApiFailure({
            updateGlobalConfig {
                category = LogGlobalConfig.CATEGORY
                name = LogGlobalConfig.LOG_DELETE_ACCUMULATED_FILE_SIZE.getName()
                value = (Integer.MIN_VALUE).toString()
            }
        }) {
            assert delegate.code == "SYS.1007"
        }

        LogGlobalProperty.LOG_RETENTION_SIZE_GB = -2
        updateGlobalConfig {
            category = LogGlobalConfig.CATEGORY
            name = LogGlobalConfig.LOG_DELETE_LAST_MODIFIED.getName()
            value = "2"
        }
        Log4jXMLModifier log4jXMLModifier = new Log4jXMLModifier()
        assert "2d" == log4jXMLModifier.getLogRetentionDays(LogConstant.MN_SERVER_LOG_ROLLINGFILE)
        assert "2d" == log4jXMLModifier.getLogRetentionDays(LogConstant.MN_API_LOG_ROLLINGFILE)

        updateGlobalConfig {
            category = LogGlobalConfig.CATEGORY
            name = LogGlobalConfig.LOG_DELETE_LAST_MODIFIED.getName()
            value = "-1"
        }
        log4jXMLModifier = new Log4jXMLModifier()
        assert "-1d" == log4jXMLModifier.getLogRetentionDays(LogConstant.MN_SERVER_LOG_ROLLINGFILE)
        assert "-1d" == log4jXMLModifier.getLogRetentionDays(LogConstant.MN_API_LOG_ROLLINGFILE)

        updateGlobalConfig {
            category = LogGlobalConfig.CATEGORY
            name = LogGlobalConfig.LOG_DELETE_ACCUMULATED_FILE_SIZE.getName()
            value = "10"
        }
        log4jXMLModifier = new Log4jXMLModifier()
        assert "8192 MB" == log4jXMLModifier.getLogRetentionSize(LogConstant.MN_SERVER_LOG_ROLLINGFILE)
        assert "2048 MB" == log4jXMLModifier.getLogRetentionSize(LogConstant.MN_API_LOG_ROLLINGFILE)

        updateGlobalConfig {
            category = LogGlobalConfig.CATEGORY
            name = LogGlobalConfig.LOG_DELETE_ACCUMULATED_FILE_SIZE.getName()
            value = "-1"
        }
        log4jXMLModifier = new Log4jXMLModifier()
        assert "-1 MB" == log4jXMLModifier.getLogRetentionSize(LogConstant.MN_SERVER_LOG_ROLLINGFILE)
        assert "-1 MB" == log4jXMLModifier.getLogRetentionSize(LogConstant.MN_API_LOG_ROLLINGFILE)

        LogGlobalProperty.LOG_RETENTION_SIZE_GB = 200
        expect(AssertionError.class){
            updateGlobalConfig {
                category = LogGlobalConfig.CATEGORY
                name = LogGlobalConfig.LOG_DELETE_ACCUMULATED_FILE_SIZE.getName()
                value = "300"
            }
        }
    }
}
