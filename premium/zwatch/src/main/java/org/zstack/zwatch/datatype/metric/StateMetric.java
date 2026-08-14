package org.zstack.zwatch.datatype.metric;

import org.zstack.zwatch.datatype.Unit;
import org.zstack.zwatch.datatype.UnitCovertRes;

import java.util.Collection;

/**
 * Created by Qi Le on 2022/1/17
 */
public class StateMetric extends Metric {
    protected StateMetric(String name, Enum... labelNames) {
        super(name, Unit.STATE, labelNames);
    }

    public StateMetric(String name, Collection collection, boolean adminOnly, Enum... labelNames) {
        this(name, collection, labelNames);
        this.adminOnly = adminOnly;
    }

    public StateMetric(String name, Collection collection, Enum... labelNames) {
        this(name, labelNames);
        collection.add(this);
    }

    @Override
    public UnitCovertRes convertUnit(double origin) {
        UnitCovertRes res = new UnitCovertRes();
        res.setValue(origin);
        return res;
    }
}
