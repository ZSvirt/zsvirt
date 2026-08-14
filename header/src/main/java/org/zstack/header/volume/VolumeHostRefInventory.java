package org.zstack.header.volume;

import org.zstack.header.host.HostInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Inventory(mappingVOClass = VolumeHostRefVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "host", inventoryClass = HostInventory.class,
                foreignKey = "hostUuid", expandedInventoryKey = "uuid")
})
public class VolumeHostRefInventory implements Serializable {
    private String hostUuid;
    private String volumeUuid;
    private String mountPath;
    private String device;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public VolumeHostRefInventory() {
    }

    public static VolumeHostRefInventory valueOf(VolumeHostRefVO vo) {
        VolumeHostRefInventory inv = new VolumeHostRefInventory();
        inv.setHostUuid(vo.getHostUuid());
        inv.setVolumeUuid(vo.getVolumeUuid());
        inv.setMountPath(vo.getMountPath());
        inv.setDevice(vo.getDevice());
        return inv;
    }

    public static List<VolumeHostRefInventory> valueOf(Collection<VolumeHostRefVO> vos) {
        return vos.stream().map(VolumeHostRefInventory::valueOf).collect(Collectors.toList());
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    public String getMountPath() {
        return mountPath;
    }

    public void setMountPath(String mountPath) {
        this.mountPath = mountPath;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
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
