package org.zstack.sso.header;

import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ToInventory;
import org.zstack.identity.imports.entity.ThirdPartyAccountSourceVO;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * @Author: DaoDao
 * @Date: 2023/2/3
 */
@Entity
@Table
@Inheritance(strategy = InheritanceType.JOINED)
public class SSOTokenVO implements ToInventory {
    @Id
    @Column
    private String uuid;

    /**
     * It is accountUuid, not userUuid
     * @see org.zstack.header.identity.AccountVO#getUuid()
     */
    @Column
    private String userUuid;

    @Column
    @ForeignKey(parentEntityClass = ThirdPartyAccountSourceVO.class, parentKey = "uuid", onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String clientUuid;

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

    public String getClientUuid() {
        return clientUuid;
    }

    public void setClientUuid(String clientUuid) {
        this.clientUuid = clientUuid;
    }

    public String getUserUuid() {
        return userUuid;
    }

    public void setUserUuid(String userUuid) {
        this.userUuid = userUuid;
    }

    public SSOTokenVO() {
    }

    protected SSOTokenVO(SSOTokenVO vo) {
        this.uuid = vo.getUuid();
        this.clientUuid = vo.getClientUuid();
        this.createDate = vo.getCreateDate();
        this.lastOpDate = vo.getLastOpDate();
        this.userUuid = vo.getUserUuid();
    }
}
