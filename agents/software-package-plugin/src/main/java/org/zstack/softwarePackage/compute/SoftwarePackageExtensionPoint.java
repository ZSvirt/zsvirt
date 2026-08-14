package org.zstack.softwarePackage.compute;

import org.zstack.header.core.Completion;
import org.zstack.header.identity.SessionInventory;
import org.zstack.softwarePackage.entity.SoftwarePackageVO;

public interface SoftwarePackageExtensionPoint {
    String getSoftwarePackageType();

    boolean isInstalledAndUnmanagedByMn();

    void installSoftwarePackage(SoftwarePackageVO softwarePackage, String config, SessionInventory session, Completion completion);

    void uninstallSoftwarePackage(SoftwarePackageVO softwarePackage, Completion completion);

    void cleanSoftwarePackage(SoftwarePackageVO softwarePackage, Completion completion);
}
