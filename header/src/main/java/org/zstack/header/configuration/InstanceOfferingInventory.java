package org.zstack.header.configuration;

import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;
import org.zstack.header.vm.VmInstanceInventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = InstanceOfferingVO.class)
@PythonClassInventory
@ExpandedQueries({
        @ExpandedQuery(expandedField = "vmInstance", inventoryClass = VmInstanceInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "instanceOfferingUuid"),
})
public class InstanceOfferingInventory implements Serializable {
    private String uuid;
    private String name;
    private String description;
    private Integer cpuNum;
    private Integer cpuSpeed;
    private Long memorySize;
    private Long reservedMemorySize;
    private String type;
    private String allocatorStrategy;
    private Integer sortKey;
    private Timestamp createDate;
    private Timestamp lastOpDate;
    private String state;

    public InstanceOfferingInventory() {
    }

    protected InstanceOfferingInventory(InstanceOfferingVO vo) {
        this.setAllocatorStrategy(vo.getAllocatorStrategy());
        this.setCpuNum(vo.getCpuNum());
        this.setCpuSpeed(vo.getCpuSpeed());
        this.setMemorySize(vo.getMemorySize());
        this.setReservedMemorySize(vo.getReservedMemorySize());
        this.setUuid(vo.getUuid());
        this.setName(vo.getName());
        this.setDescription(vo.getDescription());
        this.setSortKey(vo.getSortKey());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
        this.setType(vo.getType());
        this.setState(vo.getState().toString());
    }

    protected InstanceOfferingInventory(InstanceOfferingEO vo) {
        this.setAllocatorStrategy(vo.getAllocatorStrategy());
        this.setCpuNum(vo.getCpuNum());
        this.setCpuSpeed(vo.getCpuSpeed());
        this.setMemorySize(vo.getMemorySize());
        this.setReservedMemorySize(vo.getReservedMemorySize());
        this.setUuid(vo.getUuid());
        this.setName(vo.getName());
        this.setDescription(vo.getDescription());
        this.setSortKey(vo.getSortKey());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
        this.setType(vo.getType());
        this.setState(vo.getState().toString());
    }

    public static InstanceOfferingInventory valueOf(InstanceOfferingEO eo) {
        return new InstanceOfferingInventory(eo);
    }

    public static InstanceOfferingInventory valueOf(InstanceOfferingVO vo) {
        InstanceOfferingInventory inv = new InstanceOfferingInventory(vo);
        return inv;
    }

    public static List<InstanceOfferingInventory> valueOf(Collection<InstanceOfferingVO> vos) {
        List<InstanceOfferingInventory> invs = new ArrayList<>(vos.size());
        for (InstanceOfferingVO vo : vos) {
            invs.add(InstanceOfferingInventory.valueOf(vo));
        }
        return invs;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public int getCpuNum() {
        return cpuNum;
    }

    public void setCpuNum(int cpuNum) {
        this.cpuNum = cpuNum;
    }

    public int getCpuSpeed() {
        return cpuSpeed;
    }

    public void setCpuSpeed(int cpuSpeed) {
        this.cpuSpeed = cpuSpeed;
    }

    public long getMemorySize() {
        return memorySize;
    }

    public void setMemorySize(long memorySize) {
        this.memorySize = memorySize;
    }

    public String getAllocatorStrategy() {
        return allocatorStrategy;
    }

    public void setAllocatorStrategy(String allocatorStrategy) {
        this.allocatorStrategy = allocatorStrategy;
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

    public int getSortKey() {
        return sortKey;
    }

    public void setSortKey(int sortKey) {
        this.sortKey = sortKey;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public long getReservedMemorySize() {
        return reservedMemorySize;
    }

    public void setReservedMemorySize(long reservedMemorySize) {
        this.reservedMemorySize = reservedMemorySize;
    }
}
