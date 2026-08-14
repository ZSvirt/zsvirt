package org.zstack.autoscaling.template;

import org.zstack.header.configuration.InstanceOfferingVO;
import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vo.*;
import org.zstack.header.vo.ForeignKey;

import javax.persistence.*;

/**
 * Create by weiwang at 2018/8/16
 */
@Entity
@Table
@AutoDeleteTag
@PrimaryKeyJoinColumn(name="uuid", referencedColumnName="uuid")
public class AutoScalingVmTemplateVO extends AutoScalingTemplateVO implements ToInventory {
    @Column
    private String vmInstanceName;

    @Column
    private String vmInstanceType;

    @Column
    private String vmInstanceDescription;

    @Column
    @ForeignKey(parentEntityClass = InstanceOfferingVO.class, onDeleteAction = ForeignKey.ReferenceOption.NO_ACTION)
    private String vmInstanceOfferingUuid;

    @Column
    private String imageUuid;

    @Column
    private String l3NetworkUuids;

    @Column
    private String rootDiskOfferingUuid;

    @Column
    private String dataDiskOfferingUuids;

    @Column
    private String vmInstanceZoneUuid;

    @Column
    private String vmInstanceClusterUuid;

    @Column
    private String hostUuid;

    @Column
    private String primaryStorageUuidForRootVolume;

    @Column
    private String defaultL3NetworkUuid;

    @Column
    private String strategy;

    public String getVmInstanceOfferingUuid() {
        return vmInstanceOfferingUuid;
    }

    public void setVmInstanceOfferingUuid(String vmInstanceOfferingUuid) {
        this.vmInstanceOfferingUuid = vmInstanceOfferingUuid;
    }

    public String getImageUuid() {
        return imageUuid;
    }

    public void setImageUuid(String imageUuid) {
        this.imageUuid = imageUuid;
    }

    public String getL3NetworkUuids() {
        return l3NetworkUuids;
    }

    public void setL3NetworkUuids(String l3NetworkUuids) {
        this.l3NetworkUuids = l3NetworkUuids;
    }

    public String getRootDiskOfferingUuid() {
        return rootDiskOfferingUuid;
    }

    public void setRootDiskOfferingUuid(String rootDiskOfferingUuid) {
        this.rootDiskOfferingUuid = rootDiskOfferingUuid;
    }

    public String getDataDiskOfferingUuids() {
        return dataDiskOfferingUuids;
    }

    public void setDataDiskOfferingUuids(String dataDiskOfferingUuids) {
        this.dataDiskOfferingUuids = dataDiskOfferingUuids;
    }

    public String getVmInstanceZoneUuid() {
        return vmInstanceZoneUuid;
    }

    public void setVmInstanceZoneUuid(String vmInstanceZoneUuid) {
        this.vmInstanceZoneUuid = vmInstanceZoneUuid;
    }

    public String getVmInstanceClusterUuid() {
        return vmInstanceClusterUuid;
    }

    public void setVmInstanceClusterUuid(String vmInstanceClusterUuid) {
        this.vmInstanceClusterUuid = vmInstanceClusterUuid;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getPrimaryStorageUuidForRootVolume() {
        return primaryStorageUuidForRootVolume;
    }

    public void setPrimaryStorageUuidForRootVolume(String primaryStorageUuidForRootVolume) {
        this.primaryStorageUuidForRootVolume = primaryStorageUuidForRootVolume;
    }

    public String getDefaultL3NetworkUuid() {
        return defaultL3NetworkUuid;
    }

    public void setDefaultL3NetworkUuid(String defaultL3NetworkUuid) {
        this.defaultL3NetworkUuid = defaultL3NetworkUuid;
    }

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public String getVmInstanceName() {
        return vmInstanceName;
    }

    public void setVmInstanceName(String vmInstanceName) {
        this.vmInstanceName = vmInstanceName;
    }

    public String getVmInstanceDescription() {
        return vmInstanceDescription;
    }

    public void setVmInstanceDescription(String vmInstanceDescription) {
        this.vmInstanceDescription = vmInstanceDescription;
    }

    public String getVmInstanceType() {
        return vmInstanceType;
    }

    public void setVmInstanceType(String vmInstanceType) {
        this.vmInstanceType = vmInstanceType;
    }
}
