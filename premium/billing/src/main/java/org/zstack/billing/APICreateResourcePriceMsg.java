package org.zstack.billing;

import org.springframework.http.HttpMethod;
import org.zstack.billing.table.PriceTableVO;
import org.zstack.header.identity.AccountVO;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.tag.TagResourceType;

import java.util.Date;

/**
 * Created by frank on 2/23/2016.
 */
@TagResourceType(PriceVO.class)
@RestRequest(
        path = "/billings/prices",
        method = HttpMethod.POST,
        responseClass = APICreateResourcePriceEvent.class,
        parameterName = "params"
)
public class APICreateResourcePriceMsg extends APICreateMessage implements APIAuditor {
    @APIParam(validValues = {"cpu", "memory", "rootVolume", "dataVolume", "snapShot", "gpu",
            "pubIpVmNicBandwidthOut", "pubIpVmNicBandwidthIn", "pubIpVipBandwidthOut", "pubIpVipBandwidthIn", BillingConstants.SPENDING_TYPE_BAREMETAL2_INSTANCE})
    private String resourceName;
    @APIParam(required = false)
    private String resourceUnit;
    @APIParam
    private String timeUnit;
    @APIParam(numberRange = {0, Long.MAX_VALUE})
    private double price;
    @APIParam(resourceType = AccountVO.class, required = false)
    private String accountUuid;
    @APIParam(numberRange = {0, Long.MAX_VALUE}, required = false)
    private Long dateInLong;

    @APIParam(resourceType = PriceTableVO.class, required = false)
    private String tableUuid;

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

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }
 
    public static APICreateResourcePriceMsg __example__() {
        APICreateResourcePriceMsg msg = new APICreateResourcePriceMsg();
        msg.setTimeUnit("s");
        msg.setPrice(100d);
        msg.setResourceName(BillingConstants.SPENDING_CPU);
        msg.setDateInLong(new Date(0).getTime());

        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ? ((APICreateResourcePriceEvent)rsp).getInventory().getTableUuid() : "", PriceTableVO.class);
    }

    public String getTableUuid() {
        return tableUuid;
    }

    public void setTableUuid(String tableUuid) {
        this.tableUuid = tableUuid;
    }
}
