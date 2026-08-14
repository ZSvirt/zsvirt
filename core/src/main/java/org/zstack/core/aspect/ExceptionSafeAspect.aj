package org.zstack.core.aspect;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.errorcode.ErrorableValue;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public aspect ExceptionSafeAspect {
    private static final CLogger logger = Utils.getLogger(ExceptionSafeAspect.class);

    @Autowired
    private ErrorFacade errf;

    void around() : execution(@org.zstack.header.core.ExceptionSafe void *.*(..)) {
        try {
            proceed();
        } catch (Throwable t) {
            logger.warn("unhandled exception happened", t);
        }
    }

    ErrorableValue around() : execution(@org.zstack.header.core.ExceptionSafe ErrorableValue *.*(..)) {
        try {
            return proceed();
        } catch (Throwable t) {
            return ErrorableValue.ofErrorCode(errf.throwableToInternalError(t));
        }
    }
}
