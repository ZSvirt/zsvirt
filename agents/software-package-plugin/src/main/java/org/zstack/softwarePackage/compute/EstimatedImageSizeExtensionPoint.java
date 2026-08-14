package org.zstack.softwarePackage.compute;

public interface EstimatedImageSizeExtensionPoint {
    String getSoftwarePackageType();

    long getEstimatedImageTotalSize();
}
