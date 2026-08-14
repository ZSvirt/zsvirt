package org.zstack.softwarePackage.compute;

public interface UploadSoftwarePackageExtensionPoint {
    String resolveAndPrepareActualType(String type, String installPath, String unzipInstallPath);
}
