package org.zstack.storage.migration;

import org.zstack.header.host.HostInventory;
import org.zstack.header.volume.VolumeInventory;

import java.util.List;
import java.util.Map;

public class PrimaryStorageMigrateVmContext {
    private Map<String, List<String>> startedFlowNames;
    private String vmOriginState;
    private List<VolumeInventory> createdVolumes;
    private List<VolumeInventory> initializedVolumes;
    private HostInventory dstHostInventory;
    private Map<String, String> volumeMappingDict;
    private boolean migrateStarted;

    public Map<String, List<String>> getStartedFlowNames() {
        return startedFlowNames;
    }

    public void setStartedFlowNames(Map<String, List<String>> startedFlowNames) {
        this.startedFlowNames = startedFlowNames;
    }

    public boolean isMigrateStarted() {
        return migrateStarted;
    }

    public void setMigrateStarted(boolean migrateStarted) {
        this.migrateStarted = migrateStarted;
    }

    public List<VolumeInventory> getCreatedVolumes() {
        return createdVolumes;
    }

    public void setCreatedVolumes(List<VolumeInventory> createdVolumes) {
        this.createdVolumes = createdVolumes;
    }

    public Map<String, String> getVolumeMappingDict() {
        return volumeMappingDict;
    }

    public void setVolumeMappingDict(Map<String, String> volumeMappingDict) {
        this.volumeMappingDict = volumeMappingDict;
    }

    public HostInventory getDstHostInventory() {
        return dstHostInventory;
    }

    public void setDstHostInventory(HostInventory dstHostInventory) {
        this.dstHostInventory = dstHostInventory;
    }

    public List<VolumeInventory> getInitializedVolumes() {
        return initializedVolumes;
    }

    public void setInitializedVolumes(List<VolumeInventory> initializedVolumes) {
        this.initializedVolumes = initializedVolumes;
    }

    public String getVmOriginState() {
        return vmOriginState;
    }

    public void setVmOriginState(String vmOriginState) {
        this.vmOriginState = vmOriginState;
    }
}
