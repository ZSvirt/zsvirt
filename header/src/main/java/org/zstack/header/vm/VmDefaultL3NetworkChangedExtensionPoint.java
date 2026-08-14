package org.zstack.header.vm;

/**
 * Created by xing5 on 2016/4/18.
 */
public interface VmDefaultL3NetworkChangedExtensionPoint {
    void vmDefaultL3NetworkChanged(VmInstanceInventory vm, String previousL3, String nowL3);
}
