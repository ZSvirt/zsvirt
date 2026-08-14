package org.zstack.zsv.storage.compute;

import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.zsv.storage.api.HostSshParameter;
import org.zstack.zsv.storage.entity.CephPluginConnectionView;

import java.util.HashMap;
import java.util.Map;

public interface CephPlugin {
    String type();
    boolean testConnection(String ip);
    boolean isPackageInstalled(HostSshParameter hostSshParameter);
    default void loadProperties(CephPluginConnectionView view, ReturnValueCompletion<Map<String, Object>> completion) {
        completion.success(new HashMap<>());
    }
}
