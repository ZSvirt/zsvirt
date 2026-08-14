package org.zstack.header.host;

import org.zstack.header.message.NeedReplyMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CheckSnapshotOnHypervisorMsg extends NeedReplyMessage implements HostMessage {
    private String vmUuid;
    private String hostUuid;
    private String volumeUuid;
    private String primaryStorageUuid;
    private String currentInstallPath;
    private Map<String, Integer> volumeChainToCheck;
    private List<String> excludeInstallPaths = new ArrayList<>();

    @Override
    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    public Map<String, Integer> getVolumeChainToCheck() {
        return volumeChainToCheck;
    }

    public void setVolumeChainToCheck(Map<String, Integer> volumeChainToCheck) {
        this.volumeChainToCheck = volumeChainToCheck;
    }

    public String getVmUuid() {
        return vmUuid;
    }

    public void setVmUuid(String vmUuid) {
        this.vmUuid = vmUuid;
    }

    public String getCurrentInstallPath() {
        return currentInstallPath;
    }

    public void setCurrentInstallPath(String currentInstallPath) {
        this.currentInstallPath = currentInstallPath;
    }

    public List<String> getExcludeInstallPaths() {
        return excludeInstallPaths;
    }

    public void setExcludeInstallPaths(List<String> excludeInstallPaths) {
        this.excludeInstallPaths = excludeInstallPaths;
    }

    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }
}
