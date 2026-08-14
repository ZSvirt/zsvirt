package org.zstack.header.billing;

import org.zstack.header.message.NeedReplyMessage;

public class CalculateAccountSpendingMsg extends NeedReplyMessage {
    private String accountUuid;
    private boolean totalOnly;
    private Long dateStart;
    private Long dateEnd;

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

    public void setDateEnd(Long dateEnd) {
        this.dateEnd = dateEnd;
    }

    public boolean isTotalOnly() {
        return totalOnly;
    }

    public void setTotalOnly(boolean totalOnly) {
        this.totalOnly = totalOnly;
    }
}
