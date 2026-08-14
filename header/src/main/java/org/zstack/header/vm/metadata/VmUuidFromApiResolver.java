package org.zstack.header.vm.metadata;

import java.util.ArrayList;
import java.util.List;

public interface VmUuidFromApiResolver {
    String resolveVmUuid(String fieldValue);

    default List<String> batchResolveVmUuids(List<String> fieldValues) {
        List<String> result = new ArrayList<>();
        if (fieldValues == null || fieldValues.isEmpty()) {
            return result;
        }
        for (String v : fieldValues) {
            if (v == null) {
                continue;
            }
            String vmUuid = resolveVmUuid(v);
            if (vmUuid != null) {
                result.add(vmUuid);
            }
        }
        return result;
    }
}
