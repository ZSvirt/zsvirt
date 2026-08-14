package org.zstack.header.tag;

import org.zstack.header.message.APICreateMessage;

/**
 */
public interface SystemTagCreateMessageValidator {
    void validateSystemTagInCreateMessage(APICreateMessage msg);
}
