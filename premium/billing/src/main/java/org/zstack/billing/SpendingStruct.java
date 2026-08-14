package org.zstack.billing;

import org.zstack.header.billing.CalculateAccountSpendingMsg;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by frank on 3/4/2016.
 */
public class SpendingStruct {
    private String accountUuid;
    private Long dateStart;
    private Long dateEnd;
    private String hypervisorType;
    private String resourceUuid;
    private boolean simple;
    private List<String> resourceUuids = new ArrayList<>();
    private Integer usageLimit;

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public Long getDateStart() {
        return dateStart;
    }

    public void setDateStart(Long dateStart) {
        this.dateStart = dateStart;
    }

    public Long getDateEnd() {
        return dateEnd;
    }

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public void setDateEnd(Long dateEnd) {
        this.dateEnd = dateEnd;
    }

    public String getHypervisorType() {
        return hypervisorType;
    }

    public void setHypervisorType(String hypervisorType) {
        this.hypervisorType = hypervisorType;
    }

    public boolean isSimple() {
        return simple;
    }

    public void setSimple(boolean simple) {
        this.simple = simple;
    }

    public static SpendingStruct fromApiMessage(APICalculateAccountSpendingMsg msg) {
        SpendingStruct s = new SpendingStruct();
        s.accountUuid = msg.getAccountUuid();
        s.hypervisorType = msg.getHypervisorType();
        s.dateStart = msg.getDateStart();
        s.dateEnd = msg.getDateEnd();
        s.simple = msg.isSimple();
        return s;
    }

    public static SpendingStruct fromMessage(CalculateAccountSpendingMsg msg) {
        SpendingStruct s = new SpendingStruct();
        s.accountUuid = msg.getAccountUuid();
        s.dateStart = msg.getDateStart();
        s.dateEnd = msg.getDateEnd();
        return s;
    }

    public static SpendingStruct fromApiMessage(APICalculateAccountBillingSpendingMsg msg) {
        SpendingStruct s = new SpendingStruct();
        s.accountUuid = msg.getAccountUuid();
        s.dateStart = msg.getDateStart();
        s.dateEnd = msg.getDateEnd();
        s.resourceUuid = msg.getResourceUuid();
        s.simple = msg.isSimple();
        return s;
    }

    public List<String> getResourceUuids() {
        return resourceUuids;
    }

    public void setResourceUuids(List<String> resourceUuids) {
        this.resourceUuids = resourceUuids;
    }

    public Integer getUsageLimit() {
        return usageLimit;
    }

    public void setUsageLimit(Integer usageLimit) {
        this.usageLimit = usageLimit;
    }
}
