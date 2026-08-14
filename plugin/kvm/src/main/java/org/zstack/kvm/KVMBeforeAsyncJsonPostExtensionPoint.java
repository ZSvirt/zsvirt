package org.zstack.kvm;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Create by weiwang at 2018/6/4
 */
public interface KVMBeforeAsyncJsonPostExtensionPoint {
    LinkedHashMap kvmBeforeAsyncJsonPostExtensionPoint(String path, LinkedHashMap commandMap, Map header);
}
