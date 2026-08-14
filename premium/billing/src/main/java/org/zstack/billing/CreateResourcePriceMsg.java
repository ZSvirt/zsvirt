package org.zstack.billing;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by lining on 2019/11/12.
 */
public class CreateResourcePriceMsg extends NeedReplyMessage {
    private String priceTableUuid;

    private String resourceName;

    private String resourceUnit;

    private String timeUnit;

    private double price;

    private String accountUuid;

    private Long dateInLong;

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

    public Long getDateInLong() {
        return dateInLong;
    }

    public void setDateInLong(Long dateInLong) {
        this.dateInLong = dateInLong;
    }

    public String getPriceTableUuid() {
        return priceTableUuid;
    }

    public void setPriceTableUuid(String priceTableUuid) {
        this.priceTableUuid = priceTableUuid;
    }
}
