package org.zstack.zwatch.datatype.metric;

import org.zstack.zwatch.datatype.Unit;
import org.zstack.zwatch.datatype.UnitCovertRes;
import org.zstack.zwatch.utils.MetricUnitCoversionUtils;

import java.util.Collection;

/**
 * Created by Qi Le on 2022/1/14
 */
public class ByteRateMetric extends Metric {
    protected ByteRateMetric(String name, Enum... labelNames) {
        super(name, Unit.BYTES_PER_SECOND, labelNames);
    }

    public ByteRateMetric(String name, Collection collection, boolean adminOnly, Enum... labelNames) {
        this(name, collection, labelNames);
        this.adminOnly = adminOnly;
    }

    public ByteRateMetric(String name, Collection collection, Enum... labelNames) {
        this(name, labelNames);
        collection.add(this);
    }

    @Override
    public UnitCovertRes convertUnit(double origin) {
        UnitCovertRes res = MetricUnitCoversionUtils.convertSizeUnit(origin);
        res.setUnit(res.getUnit() + "/s");
        return res;
    }
}
