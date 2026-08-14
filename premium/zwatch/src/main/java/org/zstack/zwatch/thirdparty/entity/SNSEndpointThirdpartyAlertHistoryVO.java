package org.zstack.zwatch.thirdparty.entity;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table
@IdClass(CompositePrimaryKeyForSNSEndpointThirdpartyAlertHistoryVO.class)
public class SNSEndpointThirdpartyAlertHistoryVO {
    @Column
    @Id
    private String alertUuid;

    @Column
    @Id
    private String endpointUuid;

    @Column
    private String subscriptionUuid;

    @Column
    private Timestamp createDate;

    public String getAlertUuid() {
        return alertUuid;
    }

    public void setAlertUuid(String alertUuid) {
        this.alertUuid = alertUuid;
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

    public String getSubscriptionUuid() {
        return subscriptionUuid;
    }

    public void setSubscriptionUuid(String subscriptionUuid) {
        this.subscriptionUuid = subscriptionUuid;
    }
}
