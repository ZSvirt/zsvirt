package org.zstack.header.vm.additions;

import org.zstack.header.host.HostEO;
import org.zstack.header.host.HostVO;
import org.zstack.header.vm.VmInstanceEO;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ResourceVO;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import java.sql.Timestamp;

/**
 * Virtual Machine Host-side File Value Object
 *
 * Include: NvRam / TpmState files
 */
@Entity
@Table
@EntityGraph(
        friends = {
                @EntityGraph.Neighbour(type = VmInstanceEO.class, myField = "vmInstanceUuid", targetField = "uuid"),
                @EntityGraph.Neighbour(type = HostVO.class, myField = "hostUuid", targetField = "uuid"),
        }
)
public class VmHostFileVO extends ResourceVO {
    @Column
    @ForeignKey(parentEntityClass = VmInstanceEO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String vmInstanceUuid;
    @Column
    @ForeignKey(parentEntityClass = HostEO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String hostUuid;
    @Column
    @Enumerated(EnumType.STRING)
    private VmHostFileType type;
    @Column
    private String path;
    @Column
    private String lastSyncReason;
    @Column
    private Timestamp changeDate;
    @Column
    private Timestamp lastSyncDate;
    @Column
    private Timestamp createDate;
    @Column
    private Timestamp lastOpDate;

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

    public VmHostFileType getType() {
        return type;
    }

    public void setType(VmHostFileType type) {
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

    @Override
    public String toString() {
        return "VmHostFileVO{" +
        "uuid='" + uuid + '\'' +
        ", vmInstanceUuid='" + vmInstanceUuid + '\'' +
        ", hostUuid='" + hostUuid + '\'' +
        ", type=" + type +
        ", path='" + path + '\'' +
        ", lastSyncReason='" + lastSyncReason + '\'' +
        ", changeDate=" + changeDate +
        ", lastSyncDate=" + lastSyncDate +
        ", createDate=" + createDate +
        ", lastOpDate=" + lastOpDate +
        '}';
    }
}
