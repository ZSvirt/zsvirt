package org.zstack.twoFactorAuthentication;

import org.zstack.header.identity.OwnedByAccount;
import org.zstack.header.vo.ResourceVO;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Created by shixin on 06/26/2018.
 */
@Entity
@Table
public class TwoFactorAuthenticationSecretVO extends ResourceVO implements OwnedByAccount {

    @Column
    private String secret;

    @Column
    private String accountUuid;

    @Column
    @Enumerated(EnumType.STRING)
    private TwoFactorAuthenticationSecretStatus status;

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

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
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

    public TwoFactorAuthenticationSecretStatus getStatus() {
        return status;
    }

    public void setStatus(TwoFactorAuthenticationSecretStatus status) {
        this.status = status;
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
