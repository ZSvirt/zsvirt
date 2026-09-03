package org.zstack.resourceconfig;

import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Inventory(mappingVOClass = ResourceConfigVO.class)
public class ResourceConfigInventory {
    private String uuid;
    private String resourceUuid;
    private String resourceType;
    private String name;
    private String description;
    private String category;
    private String value;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static ResourceConfigInventory valueOf(ResourceConfigVO vo) {
        ResourceConfigInventory inv = new ResourceConfigInventory();
        inv.uuid = vo.getUuid();
        inv.resourceUuid = vo.getResourceUuid();
        inv.name = vo.getName();
        inv.description = vo.getDescription();
        inv.category = vo.getCategory();
        inv.value = vo.getValue();
        inv.createDate = vo.getCreateDate();
        inv.lastOpDate = vo.getLastOpDate();
        inv.resourceType = vo.getResourceType();
        return inv;
    }

    public static List<ResourceConfigInventory> valueOf(Collection<ResourceConfigVO> vos) {
        return vos.stream().map(ResourceConfigInventory::valueOf).collect(Collectors.toList());
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }
}
