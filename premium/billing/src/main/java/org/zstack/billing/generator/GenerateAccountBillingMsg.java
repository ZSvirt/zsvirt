package org.zstack.billing.generator;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by lining on 2019/4/25.
 */
public class GenerateAccountBillingMsg extends NeedReplyMessage {
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
}
