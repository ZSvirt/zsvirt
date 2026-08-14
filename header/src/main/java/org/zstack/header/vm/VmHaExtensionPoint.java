package org.zstack.header.vm;

/**
 * Created by lining on 2017/7/6.
 */
public interface VmHaExtensionPoint {
    void preHaStartVm(String vmUuid);
}
