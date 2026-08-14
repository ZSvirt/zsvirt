package org.zstack.test.integration.core.cloudbus

import org.aspectj.lang.ProceedingJoinPoint
import org.zstack.core.cloudbus.CloudBusCallBack
import org.zstack.header.message.Message
import org.zstack.utils.Utils
import org.zstack.utils.logging.CLogger

class CloudBusAopProxy {
    private static final CLogger logger = Utils.getLogger(CloudBusAopProxy.class)

    static enum Behavior {
        FAIL,
        TIMEOUT,
    }

    public static final String MESSAGE_ORIGINAL_SERVICE_ID = "OriginalServiceId"
    public static final String MESSAGE_BEHAVIOR = "behavior"

    private Map<Class<? extends Message>, Behavior> messages = new HashMap<Class<? extends Message>, Behavior>()

    void addMessage(Class<? extends Message> clazz, Behavior bh) {
        messages.put(clazz, bh)
    }

    void removeMessage(Class<? extends Message> clazz) {
        messages.remove(clazz)
    }

    @SuppressWarnings("unused")
    private Object singleMessageAdvice(ProceedingJoinPoint pjp, Message msg) throws Throwable {
        Behavior bh = messages.get(msg.getClass())
        if (bh == null) {
            return pjp.proceed([msg].toArray())
        }

        msg.putHeaderEntry(MESSAGE_ORIGINAL_SERVICE_ID, msg.getServiceId())
        msg.putHeaderEntry(MESSAGE_BEHAVIOR, bh.toString())
        msg.setServiceId(ManInTheMiddleService.SERVICE_ID)
        return pjp.proceed([msg].toArray())
    }

    @SuppressWarnings("unused")
    private Object singleCallbackMessageAdvice(ProceedingJoinPoint pjp, Message msg, CloudBusCallBack callback) throws Throwable {
        Behavior bh = messages.get(msg.getClass())
        if (bh == null) {
            return pjp.proceed([msg, callback].toArray())
        }

        msg.putHeaderEntry(MESSAGE_ORIGINAL_SERVICE_ID, msg.getServiceId())
        msg.putHeaderEntry(MESSAGE_BEHAVIOR, bh.toString())
        msg.setServiceId(ManInTheMiddleService.SERVICE_ID)
        return pjp.proceed([msg, callback].toArray())
    }

    @SuppressWarnings("unused")
    private <T extends Message> Object listMessageAdvice(ProceedingJoinPoint pjp, List<T> msgs) throws Throwable {
        for (Message msg : msgs) {
            Behavior bh = messages.get(msg.getClass())
            if (bh == null) {
                logger.warn(String.format("Cannot find behavior for message[%s], however, it's in a message list sent out all in one call, that means you may forget specifying behavior of this message", msg.getMessageName()))
                continue
            }

            msg.putHeaderEntry(MESSAGE_ORIGINAL_SERVICE_ID, msg.getServiceId())
            msg.putHeaderEntry(MESSAGE_BEHAVIOR, bh.toString())
            msg.setServiceId(ManInTheMiddleService.SERVICE_ID)
        }
        return pjp.proceed([msgs].toArray())
    }
}
