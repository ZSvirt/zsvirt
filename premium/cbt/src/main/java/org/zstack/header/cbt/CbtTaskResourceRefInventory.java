package org.zstack.header.cbt;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;
import org.zstack.header.vo.ResourceInventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@PythonClassInventory
@Inventory(mappingVOClass = CbtTaskResourceRefVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "task", inventoryClass = CbtTaskInventory.class,
                foreignKey = "taskUuid", expandedInventoryKey = "uuid"),
})
public class CbtTaskResourceRefInventory implements Serializable {
    private long id;
    private String taskUuid;
    private String resourceUuid;
    private String resourceType;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    protected CbtTaskResourceRefInventory(CbtTaskResourceRefVO vo) {
        this.setId(vo.getId());
        this.setTaskUuid(vo.getTaskUuid());
        this.setResourceUuid(vo.getResourceUuid());
        this.setResourceType(vo.getResourceType());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
    }

    public static CbtTaskResourceRefInventory valueOf(CbtTaskResourceRefVO vo) {
        return new CbtTaskResourceRefInventory(vo);
    }

    public static List<CbtTaskResourceRefInventory> valueOf(Collection<CbtTaskResourceRefVO> vos) {
        List<CbtTaskResourceRefInventory> invs = new ArrayList<CbtTaskResourceRefInventory>(vos.size());
        for (CbtTaskResourceRefVO vo : vos) {
            invs.add(CbtTaskResourceRefInventory.valueOf(vo));
        }
        return invs;
    }

    public CbtTaskResourceRefInventory() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTaskUuid() {
        return taskUuid;
    }

    public void setTaskUuid(String taskUuid) {
        this.taskUuid = taskUuid;
    }

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
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

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }
}
