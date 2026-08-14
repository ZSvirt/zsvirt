package org.zstack.header.sshkeypair;

import org.zstack.header.vo.*;
import org.zstack.header.vo.ForeignKey;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table
@SoftDeletionCascades({
        @SoftDeletionCascade(parent = ResourceVO.class, joinColumn = "resourceUuid"),
        @SoftDeletionCascade(parent = SshKeyPairVO.class, joinColumn = "sshKeyPairUuid")
})
public class SshKeyPairRefVO implements ToInventory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private Long id;

    @Column
    @ForeignKey(parentEntityClass = ResourceVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String resourceUuid;

    @Column
    @ForeignKey(parentEntityClass = SshKeyPairVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String sshKeyPairUuid;

    @Column
    private String resourceType;

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSshKeyPairUuid() {
        return sshKeyPairUuid;
    }

    public void setSshKeyPairUuid(String sshKeyPairUuid) {
        this.sshKeyPairUuid = sshKeyPairUuid;
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

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }
}
