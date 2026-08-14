package org.zstack.header.resourceattribute.entity;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.StringDSL;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = ResourceAttributeKeyResourceTypeVO.class)
@PythonClassInventory
public class ResourceAttributeKeyResourceTypeInventory implements Serializable {
    private String keyUuid;
    private String resourceType;
    private Timestamp createDate;

    public static ResourceAttributeKeyResourceTypeInventory valueOf(ResourceAttributeKeyResourceTypeVO vo) {
        ResourceAttributeKeyResourceTypeInventory inv = new ResourceAttributeKeyResourceTypeInventory();
        inv.setKeyUuid(vo.getKeyUuid());
        inv.setResourceType(vo.getResourceType());
        inv.setCreateDate(vo.getCreateDate());
        return inv;
    }

    public static List<ResourceAttributeKeyResourceTypeInventory> valueOf(Collection<ResourceAttributeKeyResourceTypeVO> vos) {
        return CollectionUtils.transform(vos, ResourceAttributeKeyResourceTypeInventory::valueOf);
    }

    public String getKeyUuid() {
        return keyUuid;
    }

    public void setKeyUuid(String keyUuid) {
        this.keyUuid = keyUuid;
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

    public static ResourceAttributeKeyResourceTypeInventory __example__() {
        ResourceAttributeKeyResourceTypeInventory inv = new ResourceAttributeKeyResourceTypeInventory();
        inv.setKeyUuid(StringDSL.createFixedUuid(ResourceAttributeKeyVO.class));
        inv.setResourceType("VmInstanceVO");
        inv.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        return inv;
    }
}
