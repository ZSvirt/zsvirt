package org.zstack.network.service.virtualrouter;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.header.core.ReturnValueCompletion;

/**
 * Created by xing5 on 2016/10/31.
 */
public abstract class AbstractVirtualRouterBackend {
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    protected VirtualRouterManager vrMgr;

    protected void acquireVirtualRouterVm(VirtualRouterStruct struct, ReturnValueCompletion<VirtualRouterVmInventory> completion) {
        vrMgr.acquireVirtualRouterVm(struct, completion);
    }
}
