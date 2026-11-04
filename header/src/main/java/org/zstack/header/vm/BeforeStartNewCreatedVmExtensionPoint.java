package org.zstack.header.vm;

/**
 * Created by xing5 on 2016/5/20.
 * TODO: why you do not use VmInstanceStartNewCreatedVmExtensionPoint?
 */
public interface BeforeStartNewCreatedVmExtensionPoint {
    void beforeStartNewCreatedVm(VmInstanceSpec spec);
}
