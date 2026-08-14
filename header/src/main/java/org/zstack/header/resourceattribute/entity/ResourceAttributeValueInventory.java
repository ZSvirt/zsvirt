package org.zstack.header.resourceattribute.entity;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.StringDSL;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = ResourceAttributeValueVO.class)
@PythonClassInventory
public class ResourceAttributeValueInventory implements Serializable {
    private String keyUuid;
    private ResourceAttributeKeyInventory key;
    private String value;
    private String resourceUuid;
    private String resourceType;
    private Timestamp createDate;

    public static ResourceAttributeValueInventory valueOf(ResourceAttributeValueVO vo) {
        ResourceAttributeValueInventory inv = new ResourceAttributeValueInventory();
        inv.setKeyUuid(vo.getKeyUuid());
        inv.setKey(ResourceAttributeKeyInventory.valueOf(vo.getKey()));
        inv.setValue(vo.getValue());
        inv.setResourceUuid(vo.getResourceUuid());
        inv.setResourceType(vo.getResourceType());
        inv.setCreateDate(vo.getCreateDate());
        return inv;
    }

    public static List<ResourceAttributeValueInventory> valueOf(Collection<ResourceAttributeValueVO> vos) {
        return CollectionUtils.transform(vos, ResourceAttributeValueInventory::valueOf);
    }

    public String getKeyUuid() {
        return keyUuid;
    }

    public void setKeyUuid(String keyUuid) {
        this.keyUuid = keyUuid;
    }

    public ResourceAttributeKeyInventory getKey() {
        return key;
    }

    public void setKey(ResourceAttributeKeyInventory key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public static ResourceAttributeValueInventory __example__() {
        ResourceAttributeValueInventory inventory = new ResourceAttributeValueInventory();
        inventory.setKey(ResourceAttributeKeyInventory.__example__());
        inventory.setKeyUuid(inventory.getKey().getUuid());
        inventory.setValue("Kinny");
        inventory.setResourceUuid(StringDSL.createFixedUuid(VmInstanceVO.class));
        inventory.setResourceType(VmInstanceVO.class.getSimpleName());
        inventory.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        return inventory;
    }
}
