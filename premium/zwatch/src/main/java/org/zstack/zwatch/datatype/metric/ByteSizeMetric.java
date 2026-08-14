package org.zstack.zwatch.datatype.metric;

import org.zstack.zwatch.datatype.Unit;
import org.zstack.zwatch.datatype.UnitCovertRes;
import org.zstack.zwatch.utils.MetricUnitCoversionUtils;

import java.util.Collection;

/**
 * Created by Qi Le on 2022/1/13
 */
public class ByteSizeMetric extends Metric {
    public ByteSizeMetric(String name, Collection collection, boolean adminOnly, Enum... labelNames) {
        this(name, collection, labelNames);
        this.adminOnly = adminOnly;
    }

    public ByteSizeMetric(String name, Collection collection, Enum... labelNames) {
        this(name, labelNames);
        collection.add(this);
    }

    protected ByteSizeMetric(String name, Enum... labelNames) {
        super(name, Unit.BYTES, labelNames);
    }

    @Override
    public UnitCovertRes convertUnit(double origin) {
        return MetricUnitCoversionUtils.convertSizeUnit(origin);
    }
}
