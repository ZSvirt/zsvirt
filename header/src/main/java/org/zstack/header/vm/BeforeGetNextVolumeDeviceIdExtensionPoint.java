package org.zstack.header.vm;

import java.util.List;

/**
 * Create by weiwang at 2018/10/24
 */
public interface BeforeGetNextVolumeDeviceIdExtensionPoint {
    void beforeGetNextVolumeDeviceId(String vmUuid, List<Integer> devIds);
}
