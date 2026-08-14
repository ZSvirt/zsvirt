package org.zstack.core.aspect;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.core.CoreGlobalProperty;

/**
 */
public aspect UnitTestBypassMethodAspect {
    private static final CLogger logger = Utils.getLogger(UnitTestBypassMethodAspect.class);

    Object around() : execution(@org.zstack.header.core.BypassWhenUnitTest * *.*(..)) {
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            if (!UnitTestBypassHelper.callJudger(thisJoinPoint)) {
                return proceed();
            } else {
                logger.debug(String.format("bypass %s because of unit test", thisJoinPoint.getSignature().toLongString()));
                UnitTestBypassHelper.callConsumer(thisJoinPoint);
                return null;
            }
        } else {
            return proceed();
        }
    }
}
