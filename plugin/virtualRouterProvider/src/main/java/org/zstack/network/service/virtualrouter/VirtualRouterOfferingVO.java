package org.zstack.network.service.virtualrouter;

import org.zstack.header.configuration.InstanceOfferingEO;
import org.zstack.header.configuration.InstanceOfferingVO;
import org.zstack.header.image.ImageEO;
import org.zstack.header.network.l3.L3NetworkEO;
import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vo.EO;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ForeignKey.ReferenceOption;
import org.zstack.header.zone.ZoneEO;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table
@EO(EOClazz = InstanceOfferingEO.class, needView = false)
@AutoDeleteTag
public class VirtualRouterOfferingVO extends InstanceOfferingVO {
    @Column
    @ForeignKey(parentEntityClass = L3NetworkEO.class, onDeleteAction = ReferenceOption.CASCADE)
    private String managementNetworkUuid;
    
    @Column
    @ForeignKey(parentEntityClass = L3NetworkEO.class, onDeleteAction = ReferenceOption.CASCADE)
    private String publicNetworkUuid;
    
    @Column
    @ForeignKey(parentEntityClass = ImageEO.class, onDeleteAction = ReferenceOption.CASCADE)
    private String imageUuid;
    
    @Column
    @ForeignKey(parentEntityClass = ZoneEO.class, onDeleteAction = ReferenceOption.CASCADE)
    private String zoneUuid;
    
    @Column
    private boolean isDefault;

    VirtualRouterOfferingVO(InstanceOfferingVO vo) {
    	this.setAllocatorStrategy(vo.getAllocatorStrategy());
    	this.setCpuNum(vo.getCpuNum());
    	this.setCpuSpeed(vo.getCpuSpeed());
    	this.setCreateDate(vo.getCreateDate());
    	this.setDescription(vo.getDescription());
    	this.setDuration(vo.getDuration());
    	this.setLastOpDate(vo.getLastOpDate());
    	this.setMemorySize(vo.getMemorySize());
    	this.setName(vo.getName());
        this.setState(vo.getState());
    	this.setSortKey(vo.getSortKey());
    	this.setType(vo.getType());
    	this.setUuid(vo.getUuid());
    }
    
    public VirtualRouterOfferingVO() {
    }
    
	public String getManagementNetworkUuid() {
		return managementNetworkUuid;
	}

	public void setManagementNetworkUuid(String managementNetworkUuid) {
		this.managementNetworkUuid = managementNetworkUuid;
	}

	public String getPublicNetworkUuid() {
		return publicNetworkUuid;
	}

	public void setPublicNetworkUuid(String publicNetworkUuid) {
		this.publicNetworkUuid = publicNetworkUuid;
	}

	public String getZoneUuid() {
		return zoneUuid;
	}

	public void setZoneUuid(String zoneUuid) {
		this.zoneUuid = zoneUuid;
	}

	public boolean isDefault() {
		return isDefault;
	}

	public void setDefault(boolean isDefault) {
		this.isDefault = isDefault;
	}

	public String getImageUuid() {
		return imageUuid;
	}

	public void setImageUuid(String imageUuid) {
		this.imageUuid = imageUuid;
	}
}
