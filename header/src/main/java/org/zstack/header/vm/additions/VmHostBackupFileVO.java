package org.zstack.header.vm.additions;

import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ResourceVO;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import java.sql.Timestamp;

/**
 * Virtual Machine Host-side File Value Object (Backup files)
 *
 * Include: NvRam / TpmState files
 */
@Entity
@Table
@EntityGraph(
        friends = {
                @EntityGraph.Neighbour(type = ResourceVO.class, myField = "resourceUuid", targetField = "uuid"),
        }
)
public class VmHostBackupFileVO extends ResourceVO {
    @Column
    private String resourceUuid;
    @Column
    @Enumerated(EnumType.STRING)
    private VmHostFileType type;
    @Column
    private Timestamp createDate;
    @Column
    private Timestamp lastOpDate;

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public VmHostFileType getType() {
        return type;
    }

    public void setType(VmHostFileType type) {
        this.type = type;
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
        return "VmHostBackupFileVO{" +
        "resourceUuid='" + resourceUuid + '\'' +
        ", type=" + type +
        ", createDate=" + createDate +
        ", lastOpDate=" + lastOpDate +
        '}';
    }
}
