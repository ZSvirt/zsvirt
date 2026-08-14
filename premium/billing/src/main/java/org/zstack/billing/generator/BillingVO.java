package org.zstack.billing.generator;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Created by lining on 2019/3/28.
 */
@Table
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class BillingVO {
    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column
    @Enumerated(EnumType.STRING)
    private BillingType billingType;

    @Column
    private String accountUuid;

    @Column
    private String resourceUuid;

    @Column
    private String resourceName;

    @Column
    private double spending;

    @Column
    private long startTime;

    @Column
    private long endTime;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @Column
    private String hypervisorType;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public BillingVO() {
    }

    public BillingVO(BillingVO other) {
        this.accountUuid = other.accountUuid;
        this.billingType = other.billingType;
        this.endTime = other.endTime;
        this.startTime = other.startTime;
        this.id = other.id;
        this.spending = other.spending;
        this.resourceUuid = other.resourceUuid;
        this.createDate = other.createDate;
        this.lastOpDate = other.lastOpDate;
        this.hypervisorType = other.hypervisorType;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public BillingType getBillingType() {
        return billingType;
    }

    public void setBillingType(BillingType billingType) {
        this.billingType = billingType;
    }

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public double getSpending() {
        return spending;
    }

    public void setSpending(double spending) {
        this.spending = spending;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public String getHypervisorType() {
        return hypervisorType;
    }

    public void setHypervisorType(String hypervisorType) {
        this.hypervisorType = hypervisorType;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }
}
