package org.zstack.test.integration.core

import org.zstack.core.errorcode.ErrorFacade
import org.zstack.header.errorcode.ErrorCode
import org.zstack.testlib.SubCase
import org.zstack.utils.opaque.OpaqueConstants

import static org.zstack.core.Platform.argerr
import static org.zstack.core.Platform.operr

/**
 * Created by MaJin on 2017-06-22.
 */
class ErrorCodeHashCase extends SubCase{
    @Override
    void setup() {
        INCLUDE_CORE_SERVICES = false
    }

    @Override
    void environment() {

    }

    @Override
    void test() {
        ErrorFacade errf = bean(ErrorFacade.class)
        ErrorCode err1, err2, err3, err4
        ErrorCode errl1, errl2, errl3, errl4
        err1 = operr("test error")
        err2 = operr("test error")
        err3 = argerr("test error")
        err4 = operr("test %s", "error")

        assert err1.code == "SYS.1006"
        assert err1.details == "test error"
        assert err1.i18nDetails != null
        assert err1.opaque
        assert err1.opaque["template"] == "test error"
        assert err2.code == "SYS.1006"
        assert err2.details == "test error"
        assert err2.i18nDetails != null
        assert err2.opaque
        assert err2.opaque["template"] == "test error"
        assert err3.details == "test error"
        assert err3.i18nDetails != null
        assert err3.opaque
        assert err3.opaque["template"] == "test error"
        assert err4.details == "test error"
        assert err4.i18nDetails != null
        assert err4.opaque
        assert err4.opaque["template"] == "test %s"
        assert OpaqueConstants.OPAQUE_KEY_ARG0 == "arg.0"
        assert err4.opaque["arg.0"] == "error"

        assert err1.hashCode() == err1.hashCode()
        assert err1.hashCode() == err2.hashCode()
        assert err1.hashCode() != err3.hashCode()
        assert err1.hashCode() != err4.hashCode()
        assert Objects.equals(err1, err1)
        assert Objects.equals(err1, err2)
        assert !Objects.equals(err1, err3)
        assert !Objects.equals(err1, err4)
        assert err1.i18nDetails == err3.i18nDetails


        errl1 = errf.instantiateErrorCode("SYS.1006", "test error list").withCause([err1, err2])
        errl2 = errf.instantiateErrorCode("SYS.1006", "test error list").withCause([err1, err2])
        errl3 = errf.instantiateErrorCode("SYS.1006", "test error list")
        errl4 = errf.instantiateErrorCode("SYS.1006", "test error list")

        assert errl1.hashCode() == errl1.hashCode()
        assert errl1.hashCode() == errl2.hashCode()
        assert errl1.hashCode() != errl3.hashCode()
        assert errl3.hashCode() == errl4.hashCode()
        assert Objects.equals(errl1, errl1)
        assert Objects.equals(errl1, errl2)
        assert !Objects.equals(errl1, errl3)
        assert Objects.equals(errl3, errl4)
        assert errl1.i18nDetails == null
        assert errl2.i18nDetails == null
        assert errl3.i18nDetails == null
        assert errl4.i18nDetails == null
        assert errl1.opaque
        assert errl1.opaque["template"] == "test error list"
        assert errl2.opaque
        assert errl2.opaque["template"] == "test error list"
        assert errl3.opaque
        assert errl3.opaque["template"] == "test error list"
        assert errl4.opaque
        assert errl4.opaque["template"] == "test error list"

        err2.setDetails("test error list")
        assert err2.hashCode() != errl3.hashCode()
        assert !Objects.equals(err2, errl3)

        err2.opaque["template"] = "test error list"
        assert err2.hashCode() == errl3.hashCode()
        assert Objects.equals(err2, errl3)

        errl3.setCauses(null)
        assert errl3.hashCode() == errl4.hashCode()
        assert Objects.equals(errl3, errl4)
        assert err2.hashCode() == errl3.hashCode()
        assert Objects.equals(err2, errl3)
        
        errl4.setCauses(null)
        assert errl3.hashCode() == errl4.hashCode()
        assert Objects.equals(errl3, errl4)

        assert errl1.hashCode() != errl3.hashCode()
        assert !Objects.equals(errl1, errl3)
    }

    @Override
    void clean() {

    }

}
