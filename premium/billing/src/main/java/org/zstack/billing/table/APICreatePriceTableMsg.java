package org.zstack.billing.table;

import org.springframework.http.HttpMethod;
import org.zstack.billing.APICreateResourcePriceEvent;
import org.zstack.billing.BillingConstants;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.rest.SDK;
import org.zstack.header.tag.TagResourceType;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by lining on 2019/9/10.
 */
@TagResourceType(PriceTableVO.class)
@RestRequest(
        path = "/billings/price-tables",
        method = HttpMethod.POST,
        responseClass = APICreatePriceTableEvent.class,
        parameterName = "params"
)
public class APICreatePriceTableMsg extends APICreateMessage implements APIAuditor {
    @APIParam(maxLength = 255)
    private String name;

    @APIParam(maxLength = 2048, required = false)
    private String description;

    @APIParam
    private List<Price> prices;

    @SDK
    public static class Price {
        private String resourceName;
        private String resourceUnit;
        private String timeUnit;
        private double price;
        private Long dateInLong;
        private List<String> systemTags;

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

        public Long getDateInLong() {
            return dateInLong;
        }

        public void setDateInLong(Long dateInLong) {
            this.dateInLong = dateInLong;
        }

        public List<String> getSystemTags() {
            return systemTags;
        }

        public void setSystemTags(List<String> systemTags) {
            this.systemTags = systemTags;
        }
    }

    public static APICreatePriceTableMsg __example__() {
        APICreatePriceTableMsg msg = new APICreatePriceTableMsg();
        List<Price> prices = new ArrayList<>();
        Price price = new Price();
        price.setPrice(10L);
        price.setResourceName(BillingConstants.SPENDING_CPU);
        price.setTimeUnit("s");
        prices.add(price);

        msg.setName("table_1");
        msg.setPrices(prices);
        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ? ((APICreatePriceTableEvent)rsp).getInventory().getUuid() : "", PriceTableVO.class);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Price> getPrices() {
        return prices;
    }

    public void setPrices(List<Price> prices) {
        this.prices = prices;
    }
}
