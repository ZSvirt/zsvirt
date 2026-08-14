package org.zstack.header.resourceattribute.entity;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.query.Queryable;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.search.Inventory;
import org.zstack.utils.StringDSL;

import javax.persistence.JoinColumn;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;

import static org.zstack.utils.CollectionDSL.list;
import static org.zstack.utils.CollectionUtils.transform;

@ExpandedQueries({
        @ExpandedQuery(expandedField = "types", inventoryClass = ResourceAttributeKeyResourceTypeInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "keyUuid", hidden = true),
        @ExpandedQuery(expandedField = "constraints", inventoryClass = ResourceAttributeConstraintInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "keyUuid", hidden = true),
})
@Inventory(mappingVOClass = ResourceAttributeKeyVO.class)
@PythonClassInventory
public class ResourceAttributeKeyInventory implements Serializable {
    private String uuid;
    private String name;
    private String description;
    private List<String> resourceTypes;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    @APINoSee
    @Queryable(mappingClass = ResourceAttributeKeyResourceTypeInventory.class,
            joinColumn = @JoinColumn(name = "keyUuid", referencedColumnName = "uuid"))
    private List<ResourceAttributeKeyResourceTypeInventory> types;

    @Queryable(mappingClass = ResourceAttributeConstraintInventory.class,
            joinColumn = @JoinColumn(name = "keyUuid", referencedColumnName = "uuid"))
    private List<ResourceAttributeConstraintInventory> constraints;

    public static ResourceAttributeKeyInventory valueOf(ResourceAttributeKeyVO vo) {
        ResourceAttributeKeyInventory inv = new ResourceAttributeKeyInventory();
        inv.setUuid(vo.getUuid());
        inv.setName(vo.getName());
        inv.setDescription(vo.getDescription());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        inv.setResourceTypes(transform(vo.getTypes(), ResourceAttributeKeyResourceTypeVO::getResourceType));
        inv.setTypes(ResourceAttributeKeyResourceTypeInventory.valueOf(vo.getTypes()));
        inv.setConstraints(ResourceAttributeConstraintInventory.valueOf(vo.getConstraints()));
        return inv;
    }

    public static List<ResourceAttributeKeyInventory> valueOf(Collection<ResourceAttributeKeyVO> vos) {
        return transform(vos, ResourceAttributeKeyInventory::valueOf);
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
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

    public List<String> getResourceTypes() {
        return resourceTypes;
    }

    public void setResourceTypes(List<String> resourceTypes) {
        this.resourceTypes = resourceTypes;
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

    public List<ResourceAttributeKeyResourceTypeInventory> getTypes() {
        return types;
    }

    public void setTypes(List<ResourceAttributeKeyResourceTypeInventory> types) {
        this.types = types;
    }

    public List<ResourceAttributeConstraintInventory> getConstraints() {
        return constraints;
    }

    public void setConstraints(List<ResourceAttributeConstraintInventory> constraints) {
        this.constraints = constraints;
    }

    public static ResourceAttributeKeyInventory __example__() {
        ResourceAttributeKeyInventory inventory = new ResourceAttributeKeyInventory();
        inventory.setUuid(StringDSL.createFixedUuid(ResourceAttributeKeyVO.class));
        inventory.setName("OperationsPersonnel");
        inventory.setDescription("Kinny");
        inventory.setResourceTypes(list("VmInstanceVO"));
        inventory.setConstraints(list(ResourceAttributeConstraintInventory.__example__()));
        inventory.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inventory.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        return inventory;
    }
}
