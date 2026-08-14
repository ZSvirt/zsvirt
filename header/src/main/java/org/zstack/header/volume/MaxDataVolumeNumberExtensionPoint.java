package org.zstack.header.volume;

/**
 */
public interface MaxDataVolumeNumberExtensionPoint {
    String getHypervisorTypeForMaxDataVolumeNumberExtension();

    int getMaxDataVolumeNumber();
}
