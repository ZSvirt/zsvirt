package org.zstack.header.billing;

import org.zstack.header.message.MessageReply;


public class CalculateAccountSpendingOnlyTotalReply extends MessageReply {
    private double total;

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }
}
