package org.zstack.externalbackup;

import java.util.*;

/**
 * Created by MaJin on 2019/11/30.
 */
public class ExternalBackupSpec {
    // TODO: too large in future ?
    private List<String> allHostUuids;
    private Map<String, Set<String>> allVmUuids = null;
    private Map<String, Set<String>> restVmUuids = new HashMap<>();
    private Map<String, Set<String>> allVolumeUuids = new HashMap<>();
    private Map<String, Set<String>> restVolumeUuids = new HashMap<>();
    private List<String> allBackupStorageUuids = new ArrayList<>();
    private Set<String> restBackupStorageUuids = new HashSet<>();

    private String backupName;
    private String backupDescription;
    private String backupUuid;
    private String backupInstallPath;

    private long totalSize;

    private boolean dryRun;

    private boolean allowResume;

    private String externalDeviceUuid;

    public Map<String, Set<String>> getAllVmUuids() {
        return allVmUuids;
    }

    public Set<String> getAllVmUuids(String hostUuid) {
        return allVmUuids.get(hostUuid);
    }

    public void setAllVmUuids(Map<String, Set<String>> allVmUuids) {
        this.allVmUuids = allVmUuids;
    }

    public Map<String, Set<String>> getRestVmUuids() {
        return restVmUuids;
    }

    public Set<String> getRestVmUuids(String hostUuid) {
        return restVmUuids.get(hostUuid);
    }

    public void setRestVmUuids(Map<String, Set<String>> restVmUuids) {
        this.restVmUuids = restVmUuids;
    }

    public Map<String, Set<String>> getAllVolumeUuids() {
        return allVolumeUuids;
    }

    public void setAllVolumeUuids(Map<String, Set<String>> allVolumeUuids) {
        this.allVolumeUuids = allVolumeUuids;
    }

    public Map<String, Set<String>> getRestVolumeUuids() {
        return restVolumeUuids;
    }

    public void setRestVolumeUuids(Map<String, Set<String>> restVolumeUuids) {
        this.restVolumeUuids = restVolumeUuids;
    }

    public List<String> getAllHostUuids() {
        return allHostUuids;
    }

    public void setAllHostUuids(List<String> allHostUuids) {
        this.allHostUuids = allHostUuids;
    }

    public String getBackupName() {
        return backupName;
    }

    public void setBackupName(String backupName) {
        this.backupName = backupName;
    }

    public String getBackupDescription() {
        return backupDescription;
    }

    public void setBackupDescription(String backupDescription) {
        this.backupDescription = backupDescription;
    }

    public String getBackupUuid() {
        return backupUuid;
    }

    public void setBackupUuid(String backupUuid) {
        this.backupUuid = backupUuid;
    }

    public String getBackupInstallPath() {
        return backupInstallPath;
    }

    public void setBackupInstallPath(String backupInstallPath) {
        this.backupInstallPath = backupInstallPath;
    }

    public void completeBackupVm(String hostUuid, String vmUuid) {
        restVmUuids.get(hostUuid).remove(vmUuid);
    }

    public void completeBackupVolume(String hostUuid, String volumeUuid) {
        restVolumeUuids.get(hostUuid).remove(volumeUuid);
    }

    public void completeBackupBackupStorage(String backupStorageUuid){
        restBackupStorageUuids.remove(backupStorageUuid);
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    public List<String> getAllBackupStorageUuids() {
        return allBackupStorageUuids;
    }

    public void setAllBackupStorageUuids(List<String> allBackupStorageUuids) {
        this.allBackupStorageUuids = allBackupStorageUuids;
    }

    public Set<String> getRestBackupStorageUuids() {
        return restBackupStorageUuids;
    }

    public void setRestBackupStorageUuids(Set<String> restBackupStorageUuids) {
        this.restBackupStorageUuids = restBackupStorageUuids;
    }

    public long getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(long totalSize) {
        this.totalSize = totalSize;
    }

    public boolean isAllowResume() {
        return allowResume;
    }

    public void setAllowResume(boolean allowResume) {
        this.allowResume = allowResume;
    }

    public String getExternalDeviceUuid() {
        return externalDeviceUuid;
    }

    public void setExternalDeviceUuid(String externalDeviceUuid) {
        this.externalDeviceUuid = externalDeviceUuid;
    }

    public int getProgress() {
        long total = (long) allVmUuids.values().stream().mapToInt(Set::size).sum() +
                allVolumeUuids.values().stream().mapToInt(Set::size).sum() +
                allBackupStorageUuids.size();

        long rest = (long) restVmUuids.values().stream().mapToInt(Set::size).sum() +
                restVolumeUuids.values().stream().mapToInt(Set::size).sum() +
                restBackupStorageUuids.size();
        return (int) (90 - (rest * 90 / total)) + 5;
    }
}
