package org.zstack.header.vm;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;
import org.zstack.header.zone.ZoneInventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Inventory(mappingVOClass = VmSchedHistoryVO.class)
@PythonClassInventory
@ExpandedQueries({
        @ExpandedQuery(expandedField = "zone", inventoryClass = ZoneInventory.class,
                foreignKey = "zoneUuid", expandedInventoryKey = "uuid"),
})
public class VmSchedHistoryInventory implements Serializable {
    private long id;
    private String vmInstanceUuid;
    private String accountUuid;
    private String schedType;
    private String schedReason;
    private String failReason;
    private Boolean success;
    private String lastHostUuid;
    private String destHostUuid;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    /**
     * uuid of zone this vm is in. See :ref:`ZoneInventory`
     */
    private String zoneUuid;

    public VmSchedHistoryInventory() {
    }

    public static VmSchedHistoryInventory valueOf(VmSchedHistoryVO vo) {
        VmSchedHistoryInventory inv = new VmSchedHistoryInventory();
        inv.setId(vo.getId());
        inv.setVmInstanceUuid(vo.getVmInstanceUuid());
        inv.setAccountUuid(vo.getAccountUuid());
        inv.setSchedType(vo.getSchedType());
        inv.setSchedReason(vo.getSchedReason());
        inv.setFailReason(vo.getFailReason());
        inv.setSuccess(vo.getSuccess());
        inv.setLastHostUuid(vo.getLastHostUuid());
        inv.setDestHostUuid(vo.getDestHostUuid());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        inv.setZoneUuid(vo.getZoneUuid());
        return inv;
    }

    public static List<VmSchedHistoryInventory> valueOf(Collection<VmSchedHistoryVO> vos) {
        return vos.stream().map(VmSchedHistoryInventory::valueOf).collect(Collectors.toList());
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

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public String getZoneUuid() {
        return zoneUuid;
    }

    public void setZoneUuid(String zoneUuid) {
        this.zoneUuid = zoneUuid;
    }

    public String getSchedType() {
        return schedType;
    }

    public void setSchedType(String schedType) {
        this.schedType = schedType;
    }

    public String getSchedReason() {
        return schedReason;
    }

    public void setSchedReason(String schedReason) {
        this.schedReason = schedReason;
    }

    public String getFailReason() {
        return failReason;
    }

    public void setFailReason(String failReason) {
        this.failReason = failReason;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getLastHostUuid() {
        return lastHostUuid;
    }

    public void setLastHostUuid(String lastHostUuid) {
        this.lastHostUuid = lastHostUuid;
    }

    public String getDestHostUuid() {
        return destHostUuid;
    }

    public void setDestHostUuid(String destHostUuid) {
        this.destHostUuid = destHostUuid;
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
