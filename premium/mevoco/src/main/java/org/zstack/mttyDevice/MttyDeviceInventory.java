package org.zstack.mttyDevice;

import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostVO;
import org.zstack.header.message.DocUtils;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author yu.sun
 * @date 2022/11/17 09:27
 **/
@Inventory(mappingVOClass = MttyDeviceVO.class, collectionValueOfMethod = "valueOf1")
@ExpandedQueries({
        @ExpandedQuery(expandedField = "host", inventoryClass = HostInventory.class,
                foreignKey = "hostUuid", expandedInventoryKey = "uuid")
})
public class MttyDeviceInventory implements Serializable {
    private String uuid;
    private String name;
    private String description;
    private String hostUuid;
    private MttyDeviceType type;
    private MttyDeviceState state;
    private MttyDeviceVirtStatus virtStatus;
    private Timestamp createDate;
    private Timestamp lastOpDate;
    
    public MttyDeviceInventory() {
    }
    
    public MttyDeviceInventory(MttyDeviceVO vo) {
        this.uuid = vo.getUuid();
        this.name = vo.getName();
        this.description = vo.getDescription();
        this.hostUuid = vo.getHostUuid();
        this.type = vo.getType();
        this.state = vo.getState();
        this.virtStatus = vo.getVirtStatus();
        this.createDate = vo.getCreateDate();
        this.lastOpDate = vo.getLastOpDate();
    }
    
    public static MttyDeviceInventory __example__() {
        MttyDeviceInventory inv = new MttyDeviceInventory();
        inv.setUuid(DocUtils.createFixedUuid(MttyDeviceVO.class));
        inv.setName("SE Device");
        inv.setHostUuid(DocUtils.createFixedUuid(HostVO.class));
        inv.setType(MttyDeviceType.SE_Controller);
        inv.setState(MttyDeviceState.Enabled);
        inv.setVirtStatus(MttyDeviceVirtStatus.VFIO_MDEV_VIRTUALIZABLE);
        return inv;
    }
    
    public static MttyDeviceInventory valueOf(MttyDeviceVO vo) {
        return new MttyDeviceInventory(vo);
    }
    
    public static List<MttyDeviceInventory> valueOf1(Collection<MttyDeviceVO> vos) {
        return vos.stream().map(MttyDeviceInventory::valueOf).collect(Collectors.toList());
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
    
    public String getHostUuid() {
        return hostUuid;
    }
    
    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }
    
    public MttyDeviceType getType() {
        return type;
    }
    
    public void setType(MttyDeviceType type) {
        this.type = type;
    }
    
    public MttyDeviceVirtStatus getVirtStatus() {
        return virtStatus;
    }
    
    public void setVirtStatus(MttyDeviceVirtStatus virtStatus) {
        this.virtStatus = virtStatus;
    }
    
    public MttyDeviceState getState() {
        return state;
    }
    
    public void setState(MttyDeviceState state) {
        this.state = state;
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