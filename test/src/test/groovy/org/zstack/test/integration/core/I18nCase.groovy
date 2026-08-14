package org.zstack.test.integration.core

import org.zstack.core.Platform
import org.zstack.core.errorcode.ErrorFacade
import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.core.I18nMessage
import org.zstack.testlib.SubCase

class I18nCase extends SubCase {

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
        logger.info("Test 001: Platform.i18m(\"%s\", errorCode): raw details use getDetails(), i18n details use getI18nDetails()")
        ErrorCode cause = Platform.operr("cause %s", "C1")
        I18nMessage im = Platform.i18m("%s", cause)
        assert im.getDetails() == cause.getDetails()
        assert im.getI18nDetails() == cause.getI18nDetails()

        logger.info("Test 002")
        I18nMessage im2 = I18nMessage.valueOf("hello", "你好")
        ErrorCode cause2 = Platform.operr("test-002 %s", im2)
        assert cause2.getDetails() == "test-002 hello"
        assert cause2.getI18nDetails() == "test-002 你好"

        logger.info("Test 003: ErrorFacadeImpl.instantiateErrorCode(..., i18m(...)): details formatted correctly and i18nDetails not be destroyed")
        ErrorFacade errf = bean(ErrorFacade.class)
        ErrorCode e = errf.instantiateErrorCode("SYS.1006", "parent %s", im)
        assert e.details == String.format("parent %s", cause.getDetails())
        assert e.opaque
        assert e.opaque["arg.0"] instanceof I18nMessage
        assert ((I18nMessage)e.opaque["arg.0"]).getI18nDetails() == im.getI18nDetails()

        logger.info("Test 004: ErrorCode as I18nMessage participates in formatting: ensure behavior preserved")
        ErrorCode c4 = Platform.operr("error %s", "E2")
        I18nMessage im4 = Platform.i18m("wrap[%s]", c4)
        assert im4.getDetails() == String.format("wrap[%s]", c4.getDetails())
        assert im4.getI18nDetails() == String.format("wrap[%s]", c4.getI18nDetails())
    }
}
