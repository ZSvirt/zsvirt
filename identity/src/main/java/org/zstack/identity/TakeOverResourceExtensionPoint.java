package org.zstack.identity;

import java.util.List;

public interface TakeOverResourceExtensionPoint {
    void afterTakeOverResource(List<String> resourceUuids, String originAccountUuid, String newAccountUuid);
}
