package org.zstack.header.vm.additions;

import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.SoftDeletionCascade;
import org.zstack.header.vo.SoftDeletionCascades;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;
import java.sql.Timestamp;

/**
 * Virtual Machine Host-side File Content Value Object
 */
@Entity
@Table
@SoftDeletionCascades({
        @SoftDeletionCascade(parent = ResourceVO.class, joinColumn = "uuid"),
})
public class VmHostFileContentVO {
    @Id
    @Column
    @ForeignKey(parentEntityClass = ResourceVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String uuid;
    @Column
    private byte[] content;
    @Column
    @Enumerated(EnumType.STRING)
    private VmHostFileContentFormat format;
    @Column
    private Timestamp createDate;
    @Column
    private Timestamp lastOpDate;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public VmHostFileContentFormat getFormat() {
        return format;
    }

    public void setFormat(VmHostFileContentFormat format) {
        this.format = format;
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
        return "VmHostFileContentVO{" +
        "uuid='" + uuid + '\'' +
        ", format=" + format +
        ", createDate=" + createDate +
        ", lastOpDate=" + lastOpDate +
        '}';
    }
}
