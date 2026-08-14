package org.zstack.zwatch.datatype.metric;

import org.zstack.zwatch.datatype.Unit;
import org.zstack.zwatch.datatype.UnitCovertRes;
import org.zstack.zwatch.datatype.metric.Metric;

import java.util.Collection;

/**
 * Created by Qi Le on 2022/1/13
 */
public class PercentMetric extends Metric {
    protected PercentMetric(String name, Enum... labelNames) {
        super(name, Unit.PERCENT, labelNames);
    }

    public PercentMetric(String name, Collection collection, boolean adminOnly, Enum... labelNames) {
        this(name, collection, labelNames);
        this.adminOnly = adminOnly;
    }

    public PercentMetric(String name, Collection collection, Enum... labelNames) {
        this(name, labelNames);
        collection.add(this);
    }

    @Override
    public UnitCovertRes convertUnit(double origin) {
        UnitCovertRes res = new UnitCovertRes();
        res.setValue(origin);
        res.setUnit(MetricUnitConstants.PERCENT);
        return res;
    }
}
