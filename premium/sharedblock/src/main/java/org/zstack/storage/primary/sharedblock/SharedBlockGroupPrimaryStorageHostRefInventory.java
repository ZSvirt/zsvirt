package org.zstack.storage.primary.sharedblock;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.host.HostInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;
import org.zstack.header.storage.primary.PrimaryStorageHostStatus;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@PythonClassInventory
@Inventory(mappingVOClass = SharedBlockGroupPrimaryStorageHostRefVO.class, collectionValueOfMethod = "valueOf1")
@ExpandedQueries({
        @ExpandedQuery(expandedField = "sharedBlockGroup", inventoryClass = SharedBlockGroupPrimaryStorageInventory.class,
                foreignKey = "primaryStorageUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "host", inventoryClass = HostInventory.class,
                foreignKey = "hostUuid", expandedInventoryKey = "uuid")
})
public class SharedBlockGroupPrimaryStorageHostRefInventory implements Serializable {
    private String primaryStorageUuid;

    private String hostUuid;

    private Integer hostId;

    private PrimaryStorageHostStatus status;

    private Timestamp createDate;

    private Timestamp lastOpDate;

    public SharedBlockGroupPrimaryStorageHostRefInventory() {
    }

    public SharedBlockGroupPrimaryStorageHostRefInventory(SharedBlockGroupPrimaryStorageHostRefVO vo) {
        this.primaryStorageUuid = vo.getPrimaryStorageUuid();
        this.hostUuid = vo.getHostUuid();
        this.hostId = vo.getHostId();
        this.status = vo.getStatus();
        this.createDate = vo.getCreateDate();
        this.lastOpDate = vo.getLastOpDate();
    }

    public static SharedBlockGroupPrimaryStorageHostRefInventory valueOf(SharedBlockGroupPrimaryStorageHostRefVO vo) {
        return new SharedBlockGroupPrimaryStorageHostRefInventory(vo);
    }

    public static List<SharedBlockGroupPrimaryStorageHostRefInventory> valueOf1(Collection<SharedBlockGroupPrimaryStorageHostRefVO> vos) {
        List<SharedBlockGroupPrimaryStorageHostRefInventory> invs = new ArrayList<SharedBlockGroupPrimaryStorageHostRefInventory>();
        for (SharedBlockGroupPrimaryStorageHostRefVO vo : vos) {
            invs.add(valueOf(vo));
        }

        return invs;
    }

    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public Integer getHostId() {
        return hostId;
    }

    public void setHostId(Integer hostId) {
        this.hostId = hostId;
    }

    public PrimaryStorageHostStatus getStatus() {
        return status;
    }

    public void setStatus(PrimaryStorageHostStatus status) {
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
}
