package org.zstack.compute.vm;

import org.zstack.header.vm.NewVmInstanceMessage2;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.VolumeInventory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by MaJin on 2021/3/15.
 */
public class CreateVmFromVolumeResourceSpec {
    private NewVmInstanceMessage2 apiMsg;
    private String originVolumeUuidForRootVolume;

    private ImageSpec rootVolumeImage = new ImageSpec();
    private String primaryStorageUuidForRootVolume;
    private List<String> rootVolumeSystemTags;

    private String hostUuid;
    private String clusterUuid;

    private Set<String> preAllocateClusterUuids = new HashSet<>();

    private List<VolumeInventory> dataVolumes = new ArrayList<>();
    private Set<String> dataVolumeRequiredHostUuids = new HashSet<>();
    private Set<String> dataVolumeRequiredPrimaryStorageUuids = new HashSet<>();
    private VmInstanceInventory vmInstance;

    public NewVmInstanceMessage2 getApiMsg() {
        return apiMsg;
    }

    public void setApiMsg(NewVmInstanceMessage2 apiMsg) {
        this.apiMsg = apiMsg;
    }

    public ImageSpec getRootVolumeImage() {
        return rootVolumeImage;
    }

    public void setRootVolumeImage(ImageSpec rootVolumeImage) {
        this.rootVolumeImage = rootVolumeImage;
    }

    public List<VolumeInventory> getDataVolumes() {
        return dataVolumes;
    }

    public void setDataVolumes(List<VolumeInventory> dataVolumes) {
        this.dataVolumes = dataVolumes;
    }

    public void addDataVolume(VolumeInventory dataVolume) {
        this.dataVolumes.add(dataVolume);
    }

    public String getPrimaryStorageUuidForRootVolume() {
        return primaryStorageUuidForRootVolume;
    }

    public void setPrimaryStorageUuidForRootVolume(String primaryStorageUuidForRootVolume) {
        this.primaryStorageUuidForRootVolume = primaryStorageUuidForRootVolume;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getOriginVolumeUuidForRootVolume() {
        return originVolumeUuidForRootVolume;
    }

    public void setOriginVolumeUuidForRootVolume(String originVolumeUuidForRootVolume) {
        this.originVolumeUuidForRootVolume = originVolumeUuidForRootVolume;
    }

    public VmInstanceInventory getVmInstance() {
        return vmInstance;
    }

    public void setVmInstance(VmInstanceInventory vmInstance) {
        this.vmInstance = vmInstance;
    }

    public List<String> getRootVolumeSystemTags() {
        return rootVolumeSystemTags;
    }

    public void setRootVolumeSystemTags(List<String> rootVolumeSystemTags) {
        this.rootVolumeSystemTags = rootVolumeSystemTags;
    }

    public String getClusterUuid() {
        return clusterUuid;
    }

    public void setClusterUuid(String clusterUuid) {
        this.clusterUuid = clusterUuid;
    }

    public Set<String> getDataVolumeRequiredHostUuids() {
        return dataVolumeRequiredHostUuids;
    }

    public void setDataVolumeRequiredHostUuids(Set<String> dataVolumeRequiredHostUuids) {
        this.dataVolumeRequiredHostUuids = dataVolumeRequiredHostUuids;
    }

    public Set<String> getDataVolumeRequiredPrimaryStorageUuids() {
        return dataVolumeRequiredPrimaryStorageUuids;
    }

    public void setDataVolumeRequiredPrimaryStorageUuids(Set<String> dataVolumeRequiredPrimaryStorageUuids) {
        this.dataVolumeRequiredPrimaryStorageUuids = dataVolumeRequiredPrimaryStorageUuids;
    }

    public Set<String> getPreAllocateClusterUuids() {
        return preAllocateClusterUuids;
    }

    public void setPreAllocateClusterUuids(Set<String> preAllocateClusterUuids) {
        this.preAllocateClusterUuids = preAllocateClusterUuids;
    }

    public static class ImageSpec {
        private String uuid;
        private boolean temporary;

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }

        public boolean isTemporary() {
            return temporary;
        }

        public void setTemporary(boolean temporary) {
            this.temporary = temporary;
        }

        public ImageSpec(String uuid, boolean temporary) {
            this.uuid = uuid;
            this.temporary = temporary;
        }

        public ImageSpec() {}
    }
}
