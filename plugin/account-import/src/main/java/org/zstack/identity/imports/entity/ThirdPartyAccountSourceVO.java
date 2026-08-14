package org.zstack.identity.imports.entity;

import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vo.BaseResource;
import org.zstack.header.vo.ResourceVO;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import java.sql.Timestamp;

/**
 * Extract from LdapServerVO
 *
 * Created by Wenhao.Zhang on 2024/05/31
 */
@Entity
@Table
@AutoDeleteTag
@BaseResource
public class ThirdPartyAccountSourceVO extends ResourceVO {
    @Column
    private String description;

    /**
     * ex: "ldap"
     */
    @Column
    private String type;

    @Column
    @Enumerated(value = EnumType.STRING)
    private SyncCreatedAccountStrategy createAccountStrategy;

    @Column
    @Enumerated(value = EnumType.STRING)
    private SyncDeletedAccountStrategy deleteAccountStrategy;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public SyncCreatedAccountStrategy getCreateAccountStrategy() {
        return createAccountStrategy;
    }

    public void setCreateAccountStrategy(SyncCreatedAccountStrategy createAccountStrategy) {
        this.createAccountStrategy = createAccountStrategy;
    }

    public SyncDeletedAccountStrategy getDeleteAccountStrategy() {
        return deleteAccountStrategy;
    }

    public void setDeleteAccountStrategy(SyncDeletedAccountStrategy deleteAccountStrategy) {
        this.deleteAccountStrategy = deleteAccountStrategy;
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
