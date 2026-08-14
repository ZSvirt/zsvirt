package org.zstack.tag2;

import java.util.List;
import java.util.Map;

/**
 * Created by MaJin on 2019/2/14.
 */
class AttachTagToResourcesStruct {
    String tagUuid;
    List<String> resourceUuids;
    Map<String, String> tokens;

    AttachTagToResourcesStruct(String tagUuid, List<String> resourceUuids, Map<String, String> tokens) {
        this.tagUuid = tagUuid;
        this.resourceUuids = resourceUuids;
        this.tokens = tokens;
    }
}
