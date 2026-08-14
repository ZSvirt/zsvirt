package org.zstack.billing.generator;

import org.zstack.header.search.Inventory;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by lining on 2019/5/27.
 */
@Inventory(mappingVOClass = BillingVO.class)
public class BillingInventory {
    private long id;

    private String billingType;

    private String accountUuid;

    private String resourceUuid;

    private String resourceName;

    private double spending;

    private long startTime;

    private long endTime;

    private String hypervisorType;

    private Timestamp createDate;

    private Timestamp lastOpDate;

    public BillingInventory() {

    }

    protected BillingInventory(BillingVO vo) {
        this.setAccountUuid(vo.getAccountUuid());
        this.setBillingType(vo.getBillingType().toString());
        this.setEndTime(vo.getEndTime());
        this.setStartTime(vo.getStartTime());
        this.setId(vo.getId());
        this.setSpending(vo.getSpending());
        this.setResourceUuid(vo.getResourceUuid());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
        this.setHypervisorType(vo.getHypervisorType());
        this.setResourceName(vo.getResourceName());
    }

    public static BillingInventory valueOf(BillingVO co) {
        BillingInventory inv = new BillingInventory();
        inv.setId(co.getId());
        inv.setAccountUuid(co.getAccountUuid());
        inv.setBillingType(co.getBillingType().toString());
        inv.setEndTime(co.getEndTime());
        inv.setStartTime(co.getStartTime());
        inv.setHypervisorType(co.getHypervisorType());
        inv.setResourceName(co.getResourceName());
        inv.setSpending(co.getSpending());
        inv.setResourceUuid(co.getResourceUuid());
        inv.setCreateDate(co.getCreateDate());
        inv.setLastOpDate(co.getLastOpDate());
        return inv;
    }

    public static List<BillingInventory> valueOf(Collection<BillingVO> cos) {
        return cos.stream().map(BillingInventory::valueOf).collect(Collectors.toList());
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getBillingType() {
        return billingType;
    }

    public void setBillingType(String billingType) {
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

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
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

    public String getHypervisorType() {
        return hypervisorType;
    }

    public void setHypervisorType(String hypervisorType) {
        this.hypervisorType = hypervisorType;
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
