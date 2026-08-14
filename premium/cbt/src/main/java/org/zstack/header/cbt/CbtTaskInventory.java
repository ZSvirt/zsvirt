package org.zstack.header.cbt;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.query.Queryable;
import org.zstack.header.search.Inventory;

import javax.persistence.JoinColumn;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@PythonClassInventory
@ExpandedQueries({
        @ExpandedQuery(expandedField = "resourceRef", inventoryClass = CbtTaskResourceRefInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "taskUuid"),
})
@Inventory(mappingVOClass = CbtTaskVO.class)
public class CbtTaskInventory implements Serializable {
    private String uuid;
    private String name;
    private String description;
    private CbtTaskStatus status;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    @Queryable(mappingClass = CbtTaskResourceRefInventory.class,
            joinColumn = @JoinColumn(name = "taskUuid"))
    private List<CbtTaskResourceRefInventory> resourceRefs;

    protected CbtTaskInventory(CbtTaskVO vo) {
        this.setUuid(vo.getUuid());
        this.setName(vo.getName());
        this.setDescription(vo.getDescription());
        this.setStatus(vo.getStatus());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
        this.setResourceRefs(CbtTaskResourceRefInventory.valueOf(vo.getResourceRefs()));
    }

    public static CbtTaskInventory valueOf(CbtTaskVO vo) {
        return new CbtTaskInventory(vo);
    }

    public static List<CbtTaskInventory> valueOf(Collection<CbtTaskVO> vos) {
        List<CbtTaskInventory> invs = new ArrayList<CbtTaskInventory>(vos.size());
        for (CbtTaskVO vo : vos) {
            invs.add(CbtTaskInventory.valueOf(vo));
        }
        return invs;
    }

    public CbtTaskInventory() {
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

    public CbtTaskStatus getStatus() {
        return status;
    }

    public void setStatus(CbtTaskStatus status) {
        this.status = status;
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

    public List<CbtTaskResourceRefInventory> getResourceRefs() {
        return resourceRefs;
    }

    public void setResourceRefs(List<CbtTaskResourceRefInventory> resourceRefs) {
        this.resourceRefs = resourceRefs;
    }
}
