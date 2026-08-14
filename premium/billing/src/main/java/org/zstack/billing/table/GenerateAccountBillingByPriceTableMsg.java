package org.zstack.billing.table;

import org.zstack.billing.generator.GenerateAccountBillingMsg;
import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by lining on 2019/11/12.
 */
public class GenerateAccountBillingByPriceTableMsg extends NeedReplyMessage {
    private String priceTableUuid;

    private String accountUuid;

    private String spendingType;

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public String getSpendingType() {
        return spendingType;
    }

    public void setSpendingType(String spendingType) {
        this.spendingType = spendingType;
    }

    public String getPriceTableUuid() {
        return priceTableUuid;
    }

    public void setPriceTableUuid(String priceTableUuid) {
        this.priceTableUuid = priceTableUuid;
    }
}
