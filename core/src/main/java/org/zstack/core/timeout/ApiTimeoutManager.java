package org.zstack.core.timeout;

import org.zstack.header.message.ConfigurableTimeoutMessage;
import org.zstack.header.message.Message;

/**
 * Created by frank on 2/17/2016.
 */
public interface ApiTimeoutManager {
    Long getTimeout();

    Long getTimeoutSeconds();

    long getMessageTimeout(ConfigurableTimeoutMessage msg);

    void setMessageTimeout(Message msg);
}
