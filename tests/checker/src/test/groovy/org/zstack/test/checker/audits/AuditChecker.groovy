package org.zstack.test.checker.audits

import org.junit.Test
import org.zstack.header.exception.CloudRuntimeException
import org.zstack.header.message.APIEvent
import org.zstack.header.message.APIMessage
import org.zstack.header.other.APIAuditor
import org.zstack.header.rest.RestRequest
import org.zstack.utils.DebugUtils
import org.zstack.utils.Utils
import org.zstack.utils.logging.CLogger

import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

class AuditChecker {
    private static final CLogger logger = Utils.getLogger(AuditChecker.class)

    @Test
    void testAudit() {
        logger.info("start AuditChecker")

        APIMessage.apiMessageClasses.forEach { messageClass ->
            if (!APIAuditor.class.isAssignableFrom(messageClass)) {
                logger.info("${messageClass.simpleName} - SKIP")
                return
            }

            RestRequest at = (RestRequest) messageClass.getAnnotation(RestRequest.class);
            if (at == null) {
                logger.info("${messageClass.simpleName} - SKIP")
                return
            }

            Class eventClz = at.responseClass();

            Method method
            try {
                method = eventClz.getMethod("__example__")
            } catch (NoSuchMethodException e) {
                throw new CloudRuntimeException(String.format("failed to get method of %s, %s", eventClz.getSimpleName(), DebugUtils.getStackTrace(e)))
            }

            Object event
            try {
                event = method.invoke(null)
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new CloudRuntimeException(String.format("failed to execute __example__ method of %s, %s", eventClz.getSimpleName(), DebugUtils.getStackTrace(e)))
            }

            Object message
            try {
                message = messageClass.getDeclaredConstructor().newInstance()
            } catch (InstantiationException | IllegalAccessException e) {
                throw new CloudRuntimeException(String.format("failed to create message instance, %s", DebugUtils.getStackTrace(e)))
            }

            if (!(event instanceof APIEvent)) {
                logger.info("${messageClass.simpleName} - SKIP")
                return
            }

            try {
                ((APIAuditor) message).audit((APIMessage) message, (APIEvent) event)
            } catch (Exception e) {
                throw new CloudRuntimeException(String.format("audit error: %s, %s", messageClass.getSimpleName(), DebugUtils.getStackTrace(e)))
            }

            logger.info("${messageClass.simpleName} - PASS")
        }
    }
}
