package org.zstack.header.tag;

import org.zstack.header.message.APICreateMessage;

/**
 * Created by MaJin on 2019/2/12.
 */
public interface CreateTagFromMsgExtensionPoint {
    void afterCreateTagFromMsg(APICreateMessage msg, String resourceUuid);
}
