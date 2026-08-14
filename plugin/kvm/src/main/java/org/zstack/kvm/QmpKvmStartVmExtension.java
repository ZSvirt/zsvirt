package org.zstack.kvm;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.vm.VmGlobalConfig;
import org.zstack.compute.vm.VmSystemTags;
import org.zstack.header.AbstractService;
import org.zstack.header.Component;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.tag.TagManager;

/**
 * Create by weiwang at 2018/9/6
 */
public class QmpKvmStartVmExtension implements KVMStartVmExtensionPoint{
    @Autowired
    private TagManager tagMgr;

    @Override
    public void beforeStartVmOnKvm(KVMHostInventory host,
                                   VmInstanceSpec order, KVMAgentCommands.StartVmCmd cmd) {
        if (VmSystemTags.ADDITIONAL_QMP_ADDED.getTag(order.getVmInventory().getUuid(), VmInstanceVO.class) != null) {
            VmSystemTags.ADDITIONAL_QMP_ADDED.delete(order.getVmInventory().getUuid(), VmInstanceVO.class);
        }

        if (cmd.isAdditionalQmp()) {
            tagMgr.createInherentSystemTag(order.getVmInventory().getUuid(), VmSystemTags.ADDITIONAL_QMP_ADDED.getTagFormat(), VmInstanceVO.class.getSimpleName());
        }
    }

    @Override
    public void startVmOnKvmSuccess(KVMHostInventory host, VmInstanceSpec order) {
    }

    @Override
    public void startVmOnKvmFailed(KVMHostInventory host,
                                   VmInstanceSpec order, ErrorCode err) {
    }
}