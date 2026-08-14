package org.zstack.billing;

import org.zstack.billing.spendingcalculator.pcidevice.PricePciDeviceOfferingRefVO;
import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vo.*;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by frank on 2/23/2016.
 */
@Entity
@Table
@BaseResource
@AutoDeleteTag
public class PriceVO {
    @Id
    @Column
    protected String uuid;

    @Column
    private String resourceName;
    @Column
    private long dateInLong;

    @Column
    private Long endDateInLong;

    @Column
    private String resourceUnit;
    @Column
    private String timeUnit;
    @Column
    private double price;
    @Column
    private Timestamp createDate;
    @Column
    private Timestamp lastOpDate;

    @Column
    private String tableUuid;

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "priceUuid", insertable = false, updatable = false)
    @NoView
    private Set<PricePciDeviceOfferingRefVO> pciDeviceOfferings = new HashSet<>();

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
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

    public long getDateInLong() {
        return dateInLong;
    }

    public void setDateInLong(long dateInLong) {
        this.dateInLong = dateInLong;
    }

    public String getResourceUnit() {
        return resourceUnit;
    }

    public void setResourceUnit(String resourceUnit) {
        this.resourceUnit = resourceUnit;
    }

    public String getTimeUnit() {
        return timeUnit;
    }

    public void setTimeUnit(String timeUnit) {
        this.timeUnit = timeUnit;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice( double price) {
        this.price = price;
    }

    public Set<PricePciDeviceOfferingRefVO> getPciDeviceOfferings() {
        return pciDeviceOfferings;
    }

    public void setPciDeviceOfferings(Set<PricePciDeviceOfferingRefVO> pciDeviceOfferings) {
        this.pciDeviceOfferings = pciDeviceOfferings;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public String getTableUuid() {
        return tableUuid;
    }

    public void setTableUuid(String tableUuid) {
        this.tableUuid = tableUuid;
    }

    public Long getEndDateInLong() {
        return endDateInLong;
    }

    public void setEndDateInLong(Long endDateInLong) {
        this.endDateInLong = endDateInLong;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
