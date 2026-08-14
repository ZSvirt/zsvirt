package org.zstack.header.vm;

import org.zstack.header.errorcode.ErrorCode;

import java.util.List;

public interface VmInstanceBeforeStartExtensionPoint {
    ErrorCode handleSystemTag(String vmUuid, List<String> tags);
}
