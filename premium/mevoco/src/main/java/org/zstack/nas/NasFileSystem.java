package org.zstack.nas;

import org.zstack.header.message.Message;

/**
 * Created by mingjian.deng on 2018/3/6.
 */
public interface NasFileSystem {
    void handleMessage(Message msg);
}
