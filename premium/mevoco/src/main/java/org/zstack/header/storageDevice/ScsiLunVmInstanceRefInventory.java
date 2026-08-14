package org.zstack.header.storageDevice;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.search.Inventory;
import org.zstack.header.vm.VmInstanceInventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@PythonClassInventory
@Inventory(mappingVOClass = ScsiLunVmInstanceRefVO.class, collectionValueOfMethod = "valueOf1")
@ExpandedQueries({
        @ExpandedQuery(expandedField = "scsiLun", inventoryClass = ScsiLunInventory.class,
                foreignKey = "scsiLunUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "vm", inventoryClass = VmInstanceInventory.class,
                foreignKey = "vmInstanceUuid", expandedInventoryKey = "uuid"),
})
public class ScsiLunVmInstanceRefInventory implements Serializable {
    @APINoSee
    private long id;

    private String scsiLunUuid;

    private String vmInstanceUuid;

    private Timestamp createDate;

    private Timestamp lastOpDate;

    private Integer deviceId;

    private boolean attachMultipath;

    public ScsiLunVmInstanceRefInventory() {
    }

    public ScsiLunVmInstanceRefInventory(ScsiLunVmInstanceRefVO vo) {
        this.setId(vo.getId());
        this.setVmInstanceUuid(vo.getVmInstanceUuid());
        this.setScsiLunUuid(vo.getScsiLunUuid());
        this.setDeviceId(vo.getDeviceId());
        this.setAttachMultipath(vo.isAttachMultipath());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
    }

    public static ScsiLunVmInstanceRefInventory valueOf(ScsiLunVmInstanceRefVO vo) {
        return new ScsiLunVmInstanceRefInventory(vo);
    }

    public static List<ScsiLunVmInstanceRefInventory> valueOf1(Collection<ScsiLunVmInstanceRefVO> vos) {
        List<ScsiLunVmInstanceRefInventory> invs = new ArrayList<ScsiLunVmInstanceRefInventory>();
        for (ScsiLunVmInstanceRefVO vo : vos) {
            invs.add(valueOf(vo));
        }

        return invs;
    }

    public Integer getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Integer deviceId) {
        this.deviceId = deviceId;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
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

    public boolean isAttachMultipath() {
        return attachMultipath;
    }

    public void setAttachMultipath(boolean attachMultipath) {
        this.attachMultipath = attachMultipath;
    }
}
