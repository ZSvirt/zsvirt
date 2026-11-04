package org.zstack.compute.vm;

import org.zstack.mevoco.MevocoConstants;

public class MevocoApplicationVmNicQosConfigBackend extends MevocoVmNicQosConfigBackend {
    @Override
    public String getVmInstanceType() {
        return MevocoConstants.MEVOCO_VM_TYPE;
    }
}
