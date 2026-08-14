package org.zstack.kvm.hypervisor.datatype;

import org.zstack.header.vo.*;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.Index;
import org.zstack.kvm.KVMConstant;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table
@EntityGraph(
    parents = {
        @EntityGraph.Neighbour(type = ResourceVO.class, myField = "uuid", targetField = "uuid"),
    }
)
@SoftDeletionCascades({
    @SoftDeletionCascade(parent = ResourceVO.class, joinColumn = "uuid"),
})
public class KvmHypervisorInfoVO implements ToInventory {
    /**
     * Maybe host UUID or VM UUID
     */
    @Id
    @Column
    @Index
    private String uuid;
    /**
     * "qemu-kvm"
     * @see KVMConstant#VIRTUALIZER_QEMU_KVM
     */
    @Column
    private String hypervisor;
    /**
     * hypervisor version. "4.2.0-632.g6a6222b.el7"
     */
    @Column
    private String version;
    /**
     * Is the version match with expected
     */
    @Column
    @Enumerated(EnumType.STRING)
    private HypervisorVersionState matchState;
    @Column
    private Timestamp createDate;
    @Column
    private Timestamp lastOpDate;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getHypervisor() {
        return hypervisor;
    }

    public void setHypervisor(String hypervisor) {
        this.hypervisor = hypervisor;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public HypervisorVersionState getMatchState() {
        return matchState;
    }

    public void setMatchState(HypervisorVersionState matchState) {
        this.matchState = matchState;
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
