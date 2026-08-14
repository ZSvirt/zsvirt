package org.zstack.monitoring.media;

import org.zstack.header.message.Message;

/**
 * Created by xing5 on 2017/6/11.
 */
public interface Media {
    void handleMessage(Message msg);
}
