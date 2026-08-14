package org.zstack.header.vm.metadata;

import java.util.List;

public class VmInstanceMetadataDTO {
    private String schemaVersion;
    private VmMetadataCategory vmCategory;
    private ResourceMetadata vm;
    private List<VolumeResourceMetadata> volumes;
    private List<ResourceMetadata> nics;
    private List<String> snapshots;
    private List<String> snapshotTrees;
    private List<String> snapshotGroups;
    private List<String> snapshotGroupRefs;

    public String getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(String schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public VmMetadataCategory getVmCategory() {
        return vmCategory;
    }

    public void setVmCategory(VmMetadataCategory vmCategory) {
        this.vmCategory = vmCategory;
    }

    public ResourceMetadata getVm() {
        return vm;
    }

    public void setVm(ResourceMetadata vm) {
        this.vm = vm;
    }

    public List<VolumeResourceMetadata> getVolumes() {
        return volumes;
    }

    public void setVolumes(List<VolumeResourceMetadata> volumes) {
        this.volumes = volumes;
    }

    public List<ResourceMetadata> getNics() {
        return nics;
    }

    public void setNics(List<ResourceMetadata> nics) {
        this.nics = nics;
    }

    public List<String> getSnapshots() {
        return snapshots;
    }

    public void setSnapshots(List<String> snapshots) {
        this.snapshots = snapshots;
    }

    public List<String> getSnapshotTrees() {
        return snapshotTrees;
    }

    public void setSnapshotTrees(List<String> snapshotTrees) {
        this.snapshotTrees = snapshotTrees;
    }

    public List<String> getSnapshotGroups() {
        return snapshotGroups;
    }

    public void setSnapshotGroups(List<String> snapshotGroups) {
        this.snapshotGroups = snapshotGroups;
    }

    public List<String> getSnapshotGroupRefs() {
        return snapshotGroupRefs;
    }

    public void setSnapshotGroupRefs(List<String> snapshotGroupRefs) {
        this.snapshotGroupRefs = snapshotGroupRefs;
    }
}
