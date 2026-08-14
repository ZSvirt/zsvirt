package org.zstack.billing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;

/**
 * Created by frank on 3/4/2016.
 */
public class Spending {
    private String spendingType;
    private double spending;
    private Map<String, Double> hypervisorTypeSpending = new HashMap<>();
    private Long dateStart;
    private Long dateEnd;
    private List<SpendingDetails> details = new ArrayList<>();

    public void addDetails(SpendingDetails...dd) {
        details.addAll(asList(dd));
    }

    public List<SpendingDetails> getDetails() {
        return details;
    }

    public void setDetails(List<SpendingDetails> details) {
        this.details = details;
    }

    public String getSpendingType() {
        return spendingType;
    }

    public void setSpendingType(String spendingType) {
        this.spendingType = spendingType;
    }

    public double getSpending() {
        return spending;
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

    public Map<String, Double> getHypervisorTypeSpending() {
        return hypervisorTypeSpending;
    }

    public void setHypervisorTypeSpending(Map<String, Double> hypervisorTypeSpending) {
        this.hypervisorTypeSpending = hypervisorTypeSpending;
        spending = 0;
        hypervisorTypeSpending.forEach((k,v) -> spending += v);
    }

    public void addHypervisorTypeSpending(SpendingDetails detail) {
        spending += detail.spending;
        if (detail.getHypervisorType() == null) {
            return;
        }
        hypervisorTypeSpending.merge(detail.getHypervisorType(), detail.spending, Double::sum);
    }
}
