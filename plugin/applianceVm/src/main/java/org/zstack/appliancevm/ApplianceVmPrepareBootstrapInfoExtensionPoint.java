package org.zstack.appliancevm;

import org.zstack.header.vm.VmInstanceSpec;

import java.util.Map;

/**
 * Created by xing5 on 2016/11/23.
 */
public interface ApplianceVmPrepareBootstrapInfoExtensionPoint {
    void applianceVmPrepareBootstrapInfo(VmInstanceSpec spec, Map<String, Object> info);
}
