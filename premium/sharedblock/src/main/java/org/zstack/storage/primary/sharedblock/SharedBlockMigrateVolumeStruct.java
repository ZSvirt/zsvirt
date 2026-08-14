package org.zstack.storage.primary.sharedblock;

import java.io.Serializable;

/**
 * Create by weiwang at 2018/7/2
 */
public class SharedBlockMigrateVolumeStruct implements Serializable {
    public String volumeUuid;
    public String snapshotUuid;
    public String currentInstallPath;
    public String targetInstallPath;
    public boolean safeMode = false;
    public boolean compareQcow2 = true;
    public boolean skipIfExisting = false;
    public boolean independent = false;

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    public String getSnapshotUuid() {
        return snapshotUuid;
    }

    public void setSnapshotUuid(String snapshotUuid) {
        this.snapshotUuid = snapshotUuid;
    }

    public String getCurrentInstallPath() {
        return currentInstallPath;
    }

    public void setCurrentInstallPath(String currentInstallPath) {
        this.currentInstallPath = currentInstallPath;
    }

    public String getTargetInstallPath() {
        return targetInstallPath;
    }

    public void setTargetInstallPath(String targetInstallPath) {
        this.targetInstallPath = targetInstallPath;
    }

    public boolean isSafeMode() {
        return safeMode;
    }

    public void setSafeMode(boolean safeMode) {
        this.safeMode = safeMode;
    }

    public boolean isCompareQcow2() {
        return compareQcow2;
    }

    public void setCompareQcow2(boolean compareQcow2) {
        this.compareQcow2 = compareQcow2;
    }

    public void setSkipIfExisting(boolean skipIfExisting) {
        this.skipIfExisting = skipIfExisting;
    }

    public boolean isSkipIfExisting() {
        return skipIfExisting;
    }

    public void setIndependent(boolean independent) {
        this.independent = independent;
    }

    public boolean isIndependent() {
        return independent;
    }
}
