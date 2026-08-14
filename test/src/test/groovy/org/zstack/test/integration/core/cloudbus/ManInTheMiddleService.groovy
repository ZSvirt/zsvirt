package org.zstack.test.integration.core.cloudbus

import org.springframework.beans.factory.annotation.Autowired
import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.errorcode.ErrorFacade
import org.zstack.header.AbstractService
import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.message.Message
import org.zstack.utils.Utils
import org.zstack.utils.logging.CLogger

import static org.zstack.core.Platform.operr

class ManInTheMiddleService extends AbstractService {
    private static final CLogger logger = Utils.getLogger(ManInTheMiddleService.class);

    public static final String SERVICE_ID = "ManInTheMiddle"

    @Autowired
    private CloudBus bus
    @Autowired
    private ErrorFacade errf

    @Override
    boolean start() {
        bus.registerService(this)
        return true
    }

    @Override
    boolean stop() {
        bus.unregisterService(this)
        return true
    }

    @Override
    void handleMessage(Message msg) {
        String behavior = msg.getHeaderEntry(CloudBusAopProxy.MESSAGE_BEHAVIOR)
        if (CloudBusAopProxy.Behavior.FAIL.toString() == behavior) {
            ErrorCode err = operr("unit test asks it to fail")
            bus.replyErrorByMessageType(msg, err)
        } else if (CloudBusAopProxy.Behavior.TIMEOUT.toString() == behavior) {
            logger.debug(String.format("drop message[%s, %s] as unit test ask it to time out", msg.getMessageName(), msg.getId()))
        } else {
            bus.dealWithUnknownMessage(msg)
        }
    }

    @Override
    String getId() {
        return SERVICE_ID
    }
}
