package org.zstack.zwatch.datatype.metric;

import org.zstack.zwatch.datatype.Unit;
import org.zstack.zwatch.datatype.UnitCovertRes;
import org.zstack.zwatch.datatype.metric.Metric;

import java.util.Collection;

/**
 * Created by Qi Le on 2022/1/14
 */
public class OperationRateMetric extends Metric {
    protected OperationRateMetric(String name, Enum... labelNames) {
        super(name, Unit.COUNT_PER_SECOND, labelNames);
    }

    public OperationRateMetric(String name, Collection collection, boolean adminOnly, Enum... labelNames) {
        this(name, collection, labelNames);
        this.adminOnly = adminOnly;
    }

    public OperationRateMetric(String name, Collection collection, Enum... labelNames) {
        this(name, labelNames);
        collection.add(this);
    }

    @Override
    public UnitCovertRes convertUnit(double origin) {
        UnitCovertRes res = new UnitCovertRes();
        res.setValue(origin);
        res.setUnit(MetricUnitConstants.OPERATION_PER_SECOND);
        return res;
    }
}
