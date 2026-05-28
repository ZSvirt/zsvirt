package org.zstack.header.identity.accesskey;

import org.zstack.header.core.Completion;

public interface AccessKeyHandler {
    String[] createAccessKeyAndSecret(String accountUuid);
    void deleteAccessKeyById(String accessKeyId, Completion completion);
}
