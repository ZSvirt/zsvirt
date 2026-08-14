package org.zstack.header.baremetal.preconfiguration;

import org.zstack.header.message.Message;

/**
 * Created by GuoYi on 2018-12-29.
 */
public interface Preconfiguration {
    void handleMessage(Message msg);
}
