package org.zstack.header.storageDevice;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.host.HostInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.search.Inventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@PythonClassInventory
@Inventory(mappingVOClass = ScsiLunHostRefVO.class, collectionValueOfMethod = "valueOf1")
@ExpandedQueries({
        @ExpandedQuery(expandedField = "scsiLun", inventoryClass = ScsiLunInventory.class,
                foreignKey = "scsiLunUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "host", inventoryClass = HostInventory.class,
                foreignKey = "hostUuid", expandedInventoryKey = "uuid"),
})
public class ScsiLunHostRefInventory implements Serializable {
    @APINoSee
    private long id;

    private String scsiLunUuid;

    private String hostUuid;

    private String path;

    private String hctl;

    private Timestamp createDate;

    private Timestamp lastOpDate;

    public ScsiLunHostRefInventory() {
    }

    public ScsiLunHostRefInventory(ScsiLunHostRefVO vo) {
        this.setId(vo.getId());
        this.setHostUuid(vo.getHostUuid());
        this.setScsiLunUuid(vo.getScsiLunUuid());
        this.setHctl(vo.getHctl());
        this.setPath(vo.getPath());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
    }

    public static ScsiLunHostRefInventory valueOf(ScsiLunHostRefVO vo) {
        return new ScsiLunHostRefInventory(vo);
    }

    public static List<ScsiLunHostRefInventory> valueOf1(Collection<ScsiLunHostRefVO> vos) {
        List<ScsiLunHostRefInventory> invs = new ArrayList<ScsiLunHostRefInventory>();
        for (ScsiLunHostRefVO vo : vos) {
            invs.add(valueOf(vo));
        }

        return invs;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getHctl() {
        return hctl;
    }

    public void setHctl(String hctl) {
        this.hctl = hctl;
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

    public String getScsiLunUuid() {
        return scsiLunUuid;
    }

    public void setScsiLunUuid(String scsiLunUuid) {
        this.scsiLunUuid = scsiLunUuid;
    }
}
