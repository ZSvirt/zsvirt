package org.zstack.header.protocol;

import org.zstack.header.identity.OwnedByAccount;
import org.zstack.header.vo.BaseResource;
import org.zstack.header.vo.ResourceVO;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table
@BaseResource
public class RouterAreaVO extends ResourceVO implements OwnedByAccount {
    @Column(updatable=false)
    private String areaId;

    @Column
    @Enumerated(EnumType.STRING)
    private RouterAreaType type;

    @Column
    @Enumerated(EnumType.STRING)
    private RouterAreaAuthType authentication;

    @Column
    private String password;

    @Column
    private Integer keyId;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;
    @Transient
    private String accountUuid;

    @Override
    public String getAccountUuid() {
        return accountUuid;
    }

    @Override
    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public String getAreaId() {
        return areaId;
    }

    public void setAreaId(String areaId) {
        this.areaId = areaId;
    }

    public RouterAreaType getType() {
        return type;
    }

    public void setType(RouterAreaType type) {
        this.type = type;
    }

    public RouterAreaAuthType getAuthentication() {
        return authentication;
    }

    public void setAuthentication(RouterAreaAuthType authentication) {
        this.authentication = authentication;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getKeyId() {
        return keyId;
    }

    public void setKeyId(Integer keyId) {
        this.keyId = keyId;
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
