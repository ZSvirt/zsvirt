package org.zstack.storage.device.fibreChannel;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;
import org.zstack.storage.device.iscsi.IscsiServerClusterRefInventory;
import org.zstack.storage.device.iscsi.IscsiServerVO;
import org.zstack.storage.device.iscsi.IscsiTargetInventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@PythonClassInventory
@Inventory(mappingVOClass = FiberChannelStorageVO.class, collectionValueOfMethod = "valueOf1")
@ExpandedQueries({
        @ExpandedQuery(expandedField = "fiberChannelLun", inventoryClass = FiberChannelLunInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "fiberChannelStorageUuid"),
})
public class FiberChannelStorageInventory implements Serializable {
    private String uuid;

    private String name;

    private String wwnn;

    private String state;

    private List<FiberChannelLunInventory> fiberChannelLuns;

    private Timestamp createDate;

    private Timestamp lastOpDate;

    public FiberChannelStorageInventory() {
    }

    public FiberChannelStorageInventory(FiberChannelStorageVO vo) {
        this.setUuid(vo.getUuid());
        this.setName(vo.getName());
        this.setWwnn(vo.getWwnn());
        this.setState(vo.getState());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
        this.setFiberChannelLuns(FiberChannelLunInventory.valueOf2(vo.getFiberChannelLuns()));
    }

    public static FiberChannelStorageInventory valueOf(FiberChannelStorageVO vo) {
        return new FiberChannelStorageInventory(vo);
    }

    public static List<FiberChannelStorageInventory> valueOf1(Collection<FiberChannelStorageVO> vos) {
        List<FiberChannelStorageInventory> invs = new ArrayList<FiberChannelStorageInventory>();
        for (FiberChannelStorageVO vo : vos) {
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

    public String getWwnn() {
        return wwnn;
    }

    public void setWwnn(String wwnn) {
        this.wwnn = wwnn;
    }

    public List<FiberChannelLunInventory> getFiberChannelLuns() {
        return fiberChannelLuns;
    }

    public void setFiberChannelLuns(List<FiberChannelLunInventory> fiberChannelLuns) {
        this.fiberChannelLuns = fiberChannelLuns;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
}
