package org.zstack.guesttools;

import org.zstack.header.vm.VmInstanceEO;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vo.BaseResource;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table
@BaseResource
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = VmInstanceVO.class, myField = "vmInstanceUuid", targetField = "uuid")
        }
)
public class GuestToolsStateVO {
    @Id
    @Column
    @ForeignKey(parentEntityClass = VmInstanceEO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String vmInstanceUuid;

    @Column
    @Enumerated(EnumType.STRING)
    private GuestToolsQgaState qgaState;

    @Column
    @Enumerated(EnumType.STRING)
    private GuestToolsZWatchState zwatchState;

    @Column
    private String version;

    @Column
    private String platform;

    @Column
    private String osType;

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

    public GuestToolsQgaState getQgaState() {
        return qgaState;
    }

    public void setQgaState(GuestToolsQgaState qgaState) {
        this.qgaState = qgaState;
    }

    public GuestToolsZWatchState getZwatchState() {
        return zwatchState;
    }

    public void setZwatchState(GuestToolsZWatchState zwatchState) {
        this.zwatchState = zwatchState;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getOsType() {
        return osType;
    }

    public void setOsType(String osType) {
        this.osType = osType;
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
