package org.zstack.header.vm.additions;

import org.zstack.header.host.HostVO;
import org.zstack.header.message.DocUtils;
import org.zstack.header.vm.VmInstanceVO;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;

import static org.zstack.utils.CollectionUtils.transform;

public class VmHostFileInventory {
    private String uuid;
    private String vmInstanceUuid;
    private String hostUuid;
    private String type;
    private String path;
    private String lastSyncReason;
    private Timestamp changeDate;
    private Timestamp lastSyncDate;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public VmHostFileInventory() {
    }

    public static VmHostFileInventory valueOf(VmHostFileVO vo) {
        VmHostFileInventory inv = new VmHostFileInventory();
        inv.setUuid(vo.getUuid());
        inv.setVmInstanceUuid(vo.getVmInstanceUuid());
        inv.setHostUuid(vo.getHostUuid());
        inv.setType(vo.getType().toString());
        inv.setPath(vo.getPath());
        inv.setLastSyncReason(vo.getLastSyncReason());
        inv.setChangeDate(vo.getChangeDate());
        inv.setLastSyncDate(vo.getLastSyncDate());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        return inv;
    }

    public static List<VmHostFileInventory> valueOf(Collection<VmHostFileVO> vos) {
        return transform(vos, VmHostFileInventory::valueOf);
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

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getLastSyncReason() {
        return lastSyncReason;
    }

    public void setLastSyncReason(String lastSyncReason) {
        this.lastSyncReason = lastSyncReason;
    }

    public Timestamp getChangeDate() {
        return changeDate;
    }

    public void setChangeDate(Timestamp changeDate) {
        this.changeDate = changeDate;
    }

    public Timestamp getLastSyncDate() {
        return lastSyncDate;
    }

    public void setLastSyncDate(Timestamp lastSyncDate) {
        this.lastSyncDate = lastSyncDate;
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

    public static VmHostFileInventory __example__() {
        VmHostFileInventory ref = new VmHostFileInventory();
        ref.setUuid(DocUtils.createFixedUuid(VmHostFileVO.class));
        ref.setVmInstanceUuid(DocUtils.createFixedUuid(VmInstanceVO.class));
        ref.setHostUuid(DocUtils.createFixedUuid(HostVO.class));
        ref.setType(VmHostFileType.TpmState.toString());
        ref.setPath("/var/lib/libvirt/swtpm/" + ref.getHostUuid() + "/");
        ref.setLastSyncReason("on libvirt shutdown event");
        ref.setCreateDate(DocUtils.timestamp());
        ref.setLastOpDate(DocUtils.timestamp());
        return ref;
    }
}
