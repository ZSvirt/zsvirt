package org.zstack.header.identity.extension;

import org.zstack.header.identity.SessionInventory;
import org.zstack.header.message.APIMessage;

import java.util.List;

public interface AuthorizationBackend {
    boolean takeOverAuthorization(SessionInventory session);

    APIMessage authorize(APIMessage msg);

    void validatePermission(List<Class> classes, SessionInventory session);
}
