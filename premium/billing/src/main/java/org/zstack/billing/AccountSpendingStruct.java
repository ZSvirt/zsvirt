package org.zstack.billing;

import java.util.List;

public class AccountSpendingStruct {
    private List<Spending> spendings;
    private double total;

    public List<Spending> getSpendings() {
        return spendings;
    }

    public void setSpendings(List<Spending> spendings) {
        this.spendings = spendings;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }
}
