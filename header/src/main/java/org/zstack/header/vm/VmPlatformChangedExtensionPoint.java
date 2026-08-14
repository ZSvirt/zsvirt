package org.zstack.header.vm;

/**
 * @ Author : yh.w
 * @ Date   : Created in 17:18 2020/2/27
 */
public interface VmPlatformChangedExtensionPoint {
    boolean skipPlatformChange(VmInstanceInventory vm, String previousPlatform, String nowPlatform);

    void vmPlatformChange(VmInstanceInventory vm, String previousPlatform, String nowPlatform);
}
