package org.zstack.compute.vm;

/**
 * Created by miao on 12/20/16.
 */
public interface GetNextVolumeDeviceIdExtensionPoint {
    int getNextVolumeDeviceId(String vmUuid);
}
