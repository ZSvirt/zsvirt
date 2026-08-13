package org.zstack.header.tpm.entity;

import org.zstack.header.vo.ResourceVO;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.sql.Timestamp;

/**
 * Internal holder for a TPM encryption resource key snapshot during snapshot-group revert.
 * The key material is stored in {@code EncryptedResourceKeyRefVO} with {@code resourceUuid} = this VO's uuid.
 */
@Entity
@Table
public class TpmKeyBackupVO extends ResourceVO {
    @Column
    private Timestamp createDate;
    @Column
    private Timestamp lastOpDate;

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
