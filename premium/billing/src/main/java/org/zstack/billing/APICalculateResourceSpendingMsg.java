package org.zstack.billing;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

/**
 * Created by kefeng.wang on 12/27/2018.
 */
@RestRequest(
        path = "/billings/resources/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APICalculateResourceSpendingReply.class
)
public class APICalculateResourceSpendingMsg extends APISyncCallMessage {
    @APIParam(required = false, validValues = {
            // SPENDING_TYPE_VM = SPENDING_CPU + SPENDING_MEMORY
            BillingConstants.SPENDING_TYPE_VM,
            BillingConstants.SPENDING_CPU,
            BillingConstants.SPENDING_MEMORY,

            BillingConstants.SPENDING_TYPE_ROOT_VOLUME,
            BillingConstants.SPENDING_TYPE_DATA_VOLUME,
            BillingConstants.SPENDING_TYPE_SNAPSHOT,
            BillingConstants.SPENDING_TYPE_PCI_DEVICE,

            // SPENDING_PUBLIC_IP_VIP_BANDWIDTH = SPENDING_VIP_BANDWIDTH_IN + SPENDING_VIP_BANDWIDTH_OUT
            BillingConstants.SPENDING_PUBLIC_IP_VIP_BANDWIDTH,
            BillingConstants.SPENDING_VIP_BANDWIDTH_IN,
            BillingConstants.SPENDING_VIP_BANDWIDTH_OUT,

            // SPENDING_PUBLIC_IP_VM_NIC_BANDWIDTH = SPENDING_VM_NIC_BANDWIDTH_IN + SPENDING_VM_NIC_BANDWIDTH_OUT
            BillingConstants.SPENDING_PUBLIC_IP_VM_NIC_BANDWIDTH,
            BillingConstants.SPENDING_VM_NIC_BANDWIDTH_IN,
            BillingConstants.SPENDING_VM_NIC_BANDWIDTH_OUT,

            // SPENDING_TYPE_BAREMETAL2_INSTANCE
            BillingConstants.SPENDING_TYPE_BAREMETAL2_INSTANCE,

            BillingConstants.SPENDING_TYPE_ALL, // ALL of above
    })
    private String resourceType;

    @APIParam(required = false)
    private String resourceUuid;

    @APIParam(required = false, validRegexValues = "(\\d{4})([0-1]\\d)([0-3]\\d)\\s([0-5]\\d):([0-5]\\d):([0-5]\\d)")
    private String dateStart;

    @APIParam(required = false, validRegexValues = "(\\d{4})([0-1]\\d)([0-3]\\d)\\s([0-5]\\d):([0-5]\\d):([0-5]\\d)")
    private String dateEnd;

    @APIParam(required = false, numberRange = {0, Long.MAX_VALUE})
    private Integer start;

    @APIParam(required = false, numberRange = {1, 10000})
    private Integer limit;

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public String getDateStart() {
        return dateStart;
    }

    public void setDateStart(String dateStart) {
        this.dateStart = dateStart;
    }

    public String getDateEnd() {
        return dateEnd;
    }

    public void setDateEnd(String dateEnd) {
        this.dateEnd = dateEnd;
    }

    public Integer getStart() {
        return start;
    }

    public void setStart(Integer start) {
        this.start = start;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public static APICalculateResourceSpendingMsg __example__() {
        APICalculateResourceSpendingMsg msg = new APICalculateResourceSpendingMsg();
        msg.setResourceType("VM");
        msg.setDateStart("20190102");
        msg.setDateEnd("20190304");
        msg.setStart(30);
        msg.setLimit(10);
        return msg;
    }
}
