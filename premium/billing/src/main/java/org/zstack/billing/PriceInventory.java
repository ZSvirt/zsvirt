package org.zstack.billing;

import org.zstack.billing.spendingcalculator.pcidevice.PricePciDeviceOfferingRefInventory;
import org.zstack.header.query.Queryable;
import org.zstack.header.search.Inventory;

import javax.persistence.JoinColumn;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by frank on 2/23/2016.
 */
@Inventory(mappingVOClass = PriceVO.class)
public class PriceInventory {
    private String uuid;
    private String resourceName;
    private String resourceUnit;
    private String timeUnit;
    private Double price;
    private Long dateInLong;
    private Long endDateInLong;
    private Timestamp createDate;
    private Timestamp lastOpDate;
    private String tableUuid;
    @Queryable(mappingClass = PricePciDeviceOfferingRefInventory.class,
            joinColumn = @JoinColumn(name = "priceUuid"))
    private List<PricePciDeviceOfferingRefInventory> pciDeviceOfferings;

    public static PriceInventory valueOf(PriceVO co) {
        PriceInventory inv = new PriceInventory();
        inv.setUuid(co.getUuid());
        inv.setTimeUnit(co.getTimeUnit());
        inv.setResourceName(co.getResourceName());
        inv.setPrice(co.getPrice());
        inv.setResourceUnit(co.getResourceUnit());
        inv.setDateInLong(co.getDateInLong());
        inv.setCreateDate(co.getCreateDate());
        inv.setLastOpDate(co.getLastOpDate());
        if (co.getPciDeviceOfferings() != null){
            inv.setPciDeviceOfferings(PricePciDeviceOfferingRefInventory.valueOf(co.getPciDeviceOfferings()));
        }
        inv.setTableUuid(co.getTableUuid());
        inv.setEndDateInLong(co.getEndDateInLong());
        return inv;
    }

    public static List<PriceInventory> valueOf(Collection<PriceVO> cos) {
        return cos.stream().map(PriceInventory::valueOf).collect(Collectors.toList());
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

    public Long getDateInLong() {
        return dateInLong;
    }

    public void setDateInLong(Long dateInLong) {
        this.dateInLong = dateInLong;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
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

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public List<PricePciDeviceOfferingRefInventory> getPciDeviceOfferings() {
        return pciDeviceOfferings;
    }

    public void setPciDeviceOfferings(List<PricePciDeviceOfferingRefInventory> pciDeviceOfferings) {
        this.pciDeviceOfferings = pciDeviceOfferings;
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
}
