package org.zstack.header.vm;

/**
 * Created by boce.wang on 12/14/22.
 */
public interface VmNicChangeStateExtensionPoint {
    void afterChangeVmNicState(String vmNic, String state);
}
