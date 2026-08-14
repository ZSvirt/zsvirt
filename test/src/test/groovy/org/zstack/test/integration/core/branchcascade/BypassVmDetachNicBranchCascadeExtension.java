package org.zstack.test.integration.core.branchcascade;

import org.zstack.core.cascade.AsyncBranchCascadeExtensionPoint;
import org.zstack.core.cascade.CascadeAction;
import org.zstack.header.core.Completion;
import org.zstack.header.vm.VmInstanceVO;

public class BypassVmDetachNicBranchCascadeExtension implements AsyncBranchCascadeExtensionPoint {
    public boolean success;

    @Override
    public void asyncCascade(CascadeAction action, Completion completion) {
        success = true;
        completion.success();
    }

    @Override
    public String getCascadeResourceName() {
        return VmInstanceVO.class.getSimpleName();
    }

    @Override
    public boolean skipOriginCascadeExtension(CascadeAction action) {
        return true;
    }
}
