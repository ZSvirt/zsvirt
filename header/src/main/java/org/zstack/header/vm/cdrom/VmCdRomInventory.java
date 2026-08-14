package org.zstack.header.vm.cdrom;

import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.utils.CollectionUtils;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = VmCdRomVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "vmInstance", inventoryClass = VmInstanceInventory.class,
                foreignKey = "vmInstanceUuid", expandedInventoryKey = "uuid"),
})
public class VmCdRomInventory implements Serializable {
    private String uuid;

    private String vmInstanceUuid;

    private Integer deviceId;

    private String occupant;

    private String isoUuid;

    private String isoInstallPath;

    private String name;

    private String description;

    private String protocol;

    private Timestamp createDate;

    private Timestamp lastOpDate;

    public static VmCdRomInventory valueOf(VmCdRomVO vo) {
        VmCdRomInventory inv = new VmCdRomInventory();
        inv.setUuid(vo.getUuid());
        inv.setVmInstanceUuid(vo.getVmInstanceUuid());
        inv.setDeviceId(vo.getDeviceId());
        inv.setOccupant(vo.getOccupant());
        inv.setDescription(vo.getDescription());
        inv.setName(vo.getName());
        inv.setIsoUuid(vo.getIsoUuid());
        inv.setIsoInstallPath(vo.getIsoInstallPath());
        inv.setProtocol(vo.getProtocol());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());

        return inv;
    }

    public static List<VmCdRomInventory> valueOf(Collection<VmCdRomVO> vos) {
        return CollectionUtils.transform(vos, VmCdRomInventory::valueOf);
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public int getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(int deviceId) {
        this.deviceId = deviceId;
    }

    public String getOccupant() {
        return occupant;
    }

    public void setOccupant(String occupant) {
        this.occupant = occupant;
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

    public void setDeviceId(Integer deviceId) {
        this.deviceId = deviceId;
    }

    public String getIsoUuid() {
        return isoUuid;
    }

    public void setIsoUuid(String isoUuid) {
        this.isoUuid = isoUuid;
    }

    public String getIsoInstallPath() {
        return isoInstallPath;
    }

    public void setIsoInstallPath(String isoInstallPath) {
        this.isoInstallPath = isoInstallPath;
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

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }
}
