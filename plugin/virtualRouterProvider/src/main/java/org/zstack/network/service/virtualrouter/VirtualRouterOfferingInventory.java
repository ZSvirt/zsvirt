package org.zstack.network.service.virtualrouter;

import org.zstack.appliancevm.ApplianceVmInventory;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;
import org.zstack.header.zone.ZoneInventory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = VirtualRouterOfferingVO.class, collectionValueOfMethod="valueOf1")
@PythonClassInventory
@ExpandedQueries({
        @ExpandedQuery(expandedField = "managementL3Network", inventoryClass = L3NetworkInventory.class,
                foreignKey = "managementNetworkUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "publicL3Network", inventoryClass = L3NetworkInventory.class,
                foreignKey = "publicNetworkUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "zone", inventoryClass = ZoneInventory.class,
                foreignKey = "zoneUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "image", inventoryClass = ImageInventory.class,
                foreignKey = "imageUuid", expandedInventoryKey = "uuid"),
		@ExpandedQuery(target = ApplianceVmInventory.class, expandedField = "virtualRouterOffering", inventoryClass = VirtualRouterOfferingInventory.class,
				foreignKey = "uuid", expandedInventoryKey = "instanceOfferingUuid")
})
public class VirtualRouterOfferingInventory extends InstanceOfferingInventory {
    private String managementNetworkUuid;
    private String publicNetworkUuid;
    private String zoneUuid;	
    private Boolean isDefault;
    private String imageUuid;

	public VirtualRouterOfferingInventory() {
	}

	protected VirtualRouterOfferingInventory(VirtualRouterOfferingVO vo) {
		super(vo);
		this.setManagementNetworkUuid(vo.getManagementNetworkUuid());
		this.setPublicNetworkUuid(vo.getPublicNetworkUuid());
		this.setZoneUuid(vo.getZoneUuid());
		this.setDefault(vo.isDefault());
		this.setImageUuid(vo.getImageUuid());
	}
	
	public static VirtualRouterOfferingInventory valueOf(VirtualRouterOfferingVO vo) {
		VirtualRouterOfferingInventory inv = new VirtualRouterOfferingInventory(vo);
		return inv;
	}

	public static List<VirtualRouterOfferingInventory> valueOf1(Collection<VirtualRouterOfferingVO> vos) {
	    List<VirtualRouterOfferingInventory> invs = new ArrayList<VirtualRouterOfferingInventory>(vos.size());
	    for (VirtualRouterOfferingVO vo : vos) {
	        invs.add(VirtualRouterOfferingInventory.valueOf(vo));
	    }
	    return invs;
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

	public Boolean isDefault() {
		return isDefault;
	}

	public void setDefault(Boolean isDefault) {
		this.isDefault = isDefault;
	}

	public String getImageUuid() {
		return imageUuid;
	}

	public void setImageUuid(String imageUuid) {
		this.imageUuid = imageUuid;
	}
}
