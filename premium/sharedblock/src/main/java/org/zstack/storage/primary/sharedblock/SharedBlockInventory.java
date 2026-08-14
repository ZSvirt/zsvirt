package org.zstack.storage.primary.sharedblock;

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
@Inventory(mappingVOClass = SharedBlockVO.class, collectionValueOfMethod = "valueOf1")
@ExpandedQueries({
        @ExpandedQuery(expandedField = "sharedBlockGroup", inventoryClass = SharedBlockGroupPrimaryStorageInventory.class,
                foreignKey = "sharedBlockGroupUuid", expandedInventoryKey = "uuid")
})
public class SharedBlockInventory implements Serializable {
    private String uuid;

    private String sharedBlockGroupUuid;

    private SharedBlockType type;

    private String diskUuid;

    private String name;

    private String description;

    private SharedBlockState state;

    private SharedBlockStatus status;

    private Timestamp createDate;

    private Timestamp lastOpDate;

    @Queryable(mappingClass = SharedBlockCapacityInventory.class,
            joinColumn = @JoinColumn(name = "uuid", referencedColumnName = "totalCapacity"))
    private Long totalCapacity;

    @Queryable(mappingClass = SharedBlockCapacityInventory.class,
            joinColumn = @JoinColumn(name = "uuid", referencedColumnName = "availableCapacity"))
    private Long availableCapacity;

    private String vendor;

    public SharedBlockInventory() {
    }

    public SharedBlockInventory(SharedBlockVO vo) {
        this.setUuid(vo.getUuid());
        this.setSharedBlockGroupUuid(vo.getSharedBlockGroupUuid());
        this.setDiskUuid(vo.getDiskUuid());
        this.setType(vo.getType());
        this.setDescription(vo.getDescription());
        this.setName(vo.getName());
        this.setState(vo.getState());
        this.setStatus(vo.getStatus());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
        if (vo.getCapacity() != null) {
            this.setTotalCapacity(vo.getCapacity().getTotalCapacity());
            this.setAvailableCapacity(vo.getCapacity().getAvailableCapacity());
        }
    }

    public static SharedBlockInventory valueOf(SharedBlockVO vo) {
        return new SharedBlockInventory(vo);
    }

    public static List<SharedBlockInventory> valueOf1(Collection<SharedBlockVO> vos) {
        List<SharedBlockInventory> invs = new ArrayList<SharedBlockInventory>();
        for (SharedBlockVO vo : vos) {
            invs.add(valueOf(vo));
        }

        return invs;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getSharedBlockGroupUuid() {
        return sharedBlockGroupUuid;
    }

    public void setSharedBlockGroupUuid(String sharedBlockGroupUuid) {
        this.sharedBlockGroupUuid = sharedBlockGroupUuid;
    }

    public SharedBlockType getType() {
        return type;
    }

    public void setType(SharedBlockType type) {
        this.type = type;
    }

    public String getDiskUuid() {
        return diskUuid;
    }

    public void setDiskUuid(String diskUuid) {
        this.diskUuid = diskUuid;
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

    public SharedBlockState getState() {
        return state;
    }

    public void setState(SharedBlockState state) {
        this.state = state;
    }

    public SharedBlockStatus getStatus() {
        return status;
    }

    public void setStatus(SharedBlockStatus status) {
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

    public Long getTotalCapacity() {
        return totalCapacity;
    }

    public void setTotalCapacity(Long totalCapacity) {
        this.totalCapacity = totalCapacity;
    }

    public Long getAvailableCapacity() {
        return availableCapacity;
    }

    public void setAvailableCapacity(Long availableCapacity) {
        this.availableCapacity = availableCapacity;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }
}
