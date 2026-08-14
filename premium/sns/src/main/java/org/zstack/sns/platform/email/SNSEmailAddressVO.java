package org.zstack.sns.platform.email;

import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ResourceVO;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import java.sql.Timestamp;

@Entity
@Table
public class SNSEmailAddressVO extends ResourceVO {
    @Column
    private String emailAddress;
    @Column
    @ForeignKey(parentEntityClass = SNSEmailAddressVO.class, parentKey = "uuid")
    private String endpointUuid;
    @Column
    private Timestamp createDate;
    @Column
    private Timestamp lastOpDate;

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public String getEndpointUuid() {
        return endpointUuid;
    }

    public void setEndpointUuid(String endpointUuid) {
        this.endpointUuid = endpointUuid;
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