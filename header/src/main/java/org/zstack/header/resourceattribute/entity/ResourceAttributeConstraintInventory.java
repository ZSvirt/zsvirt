package org.zstack.header.resourceattribute.entity;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.StringDSL;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = ResourceAttributeConstraintVO.class)
@PythonClassInventory
public class ResourceAttributeConstraintInventory implements Serializable {
    private long id;
    private String keyUuid;
    private String type;
    private String parameter;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static ResourceAttributeConstraintInventory valueOf(ResourceAttributeConstraintVO vo) {
        ResourceAttributeConstraintInventory inv = new ResourceAttributeConstraintInventory();
        inv.setId(vo.getId());
        inv.setKeyUuid(vo.getKeyUuid());
        inv.setType(vo.getType());
        inv.setParameter(vo.getParameter());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        return inv;
    }

    public static List<ResourceAttributeConstraintInventory> valueOf(Collection<ResourceAttributeConstraintVO> vos) {
        return CollectionUtils.transform(vos, ResourceAttributeConstraintInventory::valueOf);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getKeyUuid() {
        return keyUuid;
    }

    public void setKeyUuid(String keyUuid) {
        this.keyUuid = keyUuid;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getParameter() {
        return parameter;
    }

    public void setParameter(String parameter) {
        this.parameter = parameter;
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

    public static ResourceAttributeConstraintInventory __example__() {
        ResourceAttributeConstraintInventory inv = new ResourceAttributeConstraintInventory();
        inv.setId(1);
        inv.setKeyUuid(StringDSL.createFixedUuid(ResourceAttributeKeyVO.class));
        inv.setType("option");
        inv.setParameter("Kinny");
        inv.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inv.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        return inv;
    }
}
