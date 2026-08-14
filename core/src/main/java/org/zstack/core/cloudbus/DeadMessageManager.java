package org.zstack.core.cloudbus;

import org.zstack.header.message.Message;

public interface DeadMessageManager {
    /**
     * use to resend a message to a temporary offline management node
     *
     * @param managementNodeUuid
     * @param message
     * @param rsendFunc
     * @return true if the message is handled, otherwise false
     */
    boolean handleManagementNodeNotFoundError(String managementNodeUuid, Message message, Runnable rsendFunc);
}
