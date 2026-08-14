package org.zstack.header.storage.addon.primary;

import org.zstack.header.core.ReturnValueCompletion;

import java.util.LinkedHashMap;

public interface ExternalPrimaryStorageSvcBuilder {
    PrimaryStorageControllerSvc buildControllerSvc(ExternalPrimaryStorageVO vo);

    PrimaryStorageNodeSvc buildNodeSvc(ExternalPrimaryStorageVO vo);

    void discover(String url, String config, ReturnValueCompletion<LinkedHashMap> completion);

    String getIdentity();
}
