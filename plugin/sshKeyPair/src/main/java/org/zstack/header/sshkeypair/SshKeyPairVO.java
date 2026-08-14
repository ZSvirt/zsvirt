package org.zstack.header.sshkeypair;

import org.zstack.header.identity.OwnedByAccount;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ToInventory;
import org.zstack.header.zone.ZoneVO;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table
public class SshKeyPairVO extends ResourceVO implements OwnedByAccount, ToInventory {
    @Column
    private String name;
    @Column
    private String description;
    @Column
    private String publicKey;
    @Column
    private Timestamp createDate;
    @Column
    private Timestamp lastOpDate;
    @Transient
    private String accountUuid;

    @PreUpdate
    private void preUpdate() {lastOpDate = null; }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String keyPair) {
        this.publicKey = keyPair;
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
    public String getAccountUuid() {
        return accountUuid;
    }

    @Override
    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }
}
