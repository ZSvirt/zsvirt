package org.zstack.mevoco;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = ShareableVolumeVmInstanceRefVO.class, collectionValueOfMethod = "valueOf1")
@PythonClassInventory
public class ShareableVolumeVmInstanceRefInventory {
    private String uuid;
    private String volumeUuid;
    private String vmInstanceUuid;
    private Integer deviceId;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static ShareableVolumeVmInstanceRefInventory valueOf(ShareableVolumeVmInstanceRefVO vo) {
        ShareableVolumeVmInstanceRefInventory inv = new ShareableVolumeVmInstanceRefInventory();
        inv.setUuid(vo.getUuid());
        inv.setVolumeUuid(vo.getVolumeUuid());
        inv.setVmInstanceUuid(vo.getVmInstanceUuid());
        inv.setDeviceId(vo.getDeviceId());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        return inv;
    }

    public static List<ShareableVolumeVmInstanceRefInventory> valueOf1(Collection<ShareableVolumeVmInstanceRefVO> vos) {
        List<ShareableVolumeVmInstanceRefInventory> invs = new ArrayList<>(vos.size());
        for (ShareableVolumeVmInstanceRefVO vo : vos) {
            invs.add(ShareableVolumeVmInstanceRefInventory.valueOf(vo));
        }
        return invs;
    }

    public Integer getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Integer deviceId) {
        this.deviceId = deviceId;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
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
}
