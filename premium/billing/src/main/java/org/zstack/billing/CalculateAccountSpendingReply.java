package org.zstack.billing;

import org.zstack.header.billing.CalculateAccountSpendingOnlyTotalReply;

import java.util.List;

public class CalculateAccountSpendingReply extends CalculateAccountSpendingOnlyTotalReply {
    private List<Spending> spendings;

    public List<Spending> getSpendings() {
        return spendings;
    }

    public void setSpendings(List<Spending> spendings) {
        this.spendings = spendings;
    }
}
