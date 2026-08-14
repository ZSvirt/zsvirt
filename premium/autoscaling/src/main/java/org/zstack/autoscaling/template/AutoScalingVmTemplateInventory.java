package org.zstack.autoscaling.template;

import org.zstack.autoscaling.AutoScalingConstants;
import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;
import org.zstack.header.tag.SystemTagVO;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Create by weiwang at 2018/8/15
 */
@PythonClassInventory
@Inventory(mappingVOClass = AutoScalingVmTemplateVO.class, collectionValueOfMethod = "valueOf2")
public class AutoScalingVmTemplateInventory extends AutoScalingTemplateInventory implements Serializable {
    private String vmInstanceName;

    private String vmInstanceType;

    private String vmInstanceDescription;

    private String vmInstanceOfferingUuid;

    private String imageUuid;

    private List<String> l3NetworkUuids;

    private String rootDiskOfferingUuid;

    private List<String> dataDiskOfferingUuids;

    private String vmInstanceZoneUuid;

    private String vmInstanceClusterUuid;

    private String hostUuid;

    private String primaryStorageUuidForRootVolume;

    private String defaultL3NetworkUuid;

    private String strategy;

    public AutoScalingVmTemplateInventory() {
    }

    public AutoScalingVmTemplateInventory(AutoScalingVmTemplateVO vo) {
        super(vo);
        this.setUuid(vo.getUuid());
        this.setName(vo.getResourceName());
        this.setDescription(vo.getDescription());
        this.setVmInstanceOfferingUuid(vo.getVmInstanceOfferingUuid());
        this.setImageUuid(vo.getImageUuid());
        this.setType(vo.getType());
        this.setRootDiskOfferingUuid(vo.getRootDiskOfferingUuid());
        this.setVmInstanceZoneUuid(vo.getVmInstanceZoneUuid());
        this.setVmInstanceClusterUuid(vo.getVmInstanceClusterUuid());
        this.setHostUuid(vo.getHostUuid());
        this.setPrimaryStorageUuidForRootVolume(vo.getPrimaryStorageUuidForRootVolume());
        this.setDefaultL3NetworkUuid(vo.getDefaultL3NetworkUuid());
        this.setStrategy(vo.getStrategy());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
        this.setVmInstanceName(vo.getVmInstanceName());
        this.setVmInstanceType(vo.getVmInstanceType());
        this.setVmInstanceDescription(vo.getVmInstanceDescription());

        this.setL3NetworkUuids(new ArrayList<>());
        if (vo.getL3NetworkUuids() != null && !vo.getL3NetworkUuids().isEmpty() && !vo.getL3NetworkUuids().contains(AutoScalingConstants.AutoScalingTemplate.VmInstance.SEPARATOR)) {
            this.setL3NetworkUuids(Arrays.asList(vo.getL3NetworkUuids().split(AutoScalingConstants.AutoScalingTemplate.VmInstance.SEPARATOR)));
        }

        this.setDataDiskOfferingUuids(new ArrayList<>());
        if (vo.getDataDiskOfferingUuids() != null && !vo.getDataDiskOfferingUuids().isEmpty() && !vo.getDataDiskOfferingUuids().contains(AutoScalingConstants.AutoScalingTemplate.VmInstance.SEPARATOR)) {
            this.setDataDiskOfferingUuids(Arrays.asList(vo.getDataDiskOfferingUuids().split(AutoScalingConstants.AutoScalingTemplate.VmInstance.SEPARATOR)));
        }
    }

    public static AutoScalingVmTemplateInventory valueOf(AutoScalingVmTemplateVO vo) {
        return new AutoScalingVmTemplateInventory(vo);
    }

    public static List<AutoScalingVmTemplateInventory> valueOf2(Collection<AutoScalingVmTemplateVO> vos) {
        List<AutoScalingVmTemplateInventory> invs = new ArrayList<AutoScalingVmTemplateInventory>();
        for (AutoScalingVmTemplateVO vo : vos) {
            invs.add(valueOf(vo));
        }

        return invs;
    }

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

    public List<String> getL3NetworkUuids() {
        return l3NetworkUuids;
    }

    public void setL3NetworkUuids(List<String> l3NetworkUuids) {
        this.l3NetworkUuids = l3NetworkUuids;
    }

    public String getRootDiskOfferingUuid() {
        return rootDiskOfferingUuid;
    }

    public void setRootDiskOfferingUuid(String rootDiskOfferingUuid) {
        this.rootDiskOfferingUuid = rootDiskOfferingUuid;
    }

    public List<String> getDataDiskOfferingUuids() {
        return dataDiskOfferingUuids;
    }

    public void setDataDiskOfferingUuids(List<String> dataDiskOfferingUuids) {
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

    public String getVmInstanceType() {
        return vmInstanceType;
    }

    public void setVmInstanceType(String vmInstanceType) {
        this.vmInstanceType = vmInstanceType;
    }

    public String getVmInstanceDescription() {
        return vmInstanceDescription;
    }

    public void setVmInstanceDescription(String vmInstanceDescription) {
        this.vmInstanceDescription = vmInstanceDescription;
    }
}
