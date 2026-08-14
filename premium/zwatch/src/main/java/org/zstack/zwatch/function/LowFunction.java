package org.zstack.zwatch.function;

import org.zstack.zwatch.datatype.Datapoint;
import org.zstack.zwatch.datatype.Function;
import org.zstack.zwatch.datatype.MetricQueryObject;

import java.util.*;

public class LowFunction extends ExtremumFunction implements MetricFunction {
    public static final String NAME = "low";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Double getExtremum(List<Datapoint> dps) {
        return dps.stream().min(Comparator.comparing(Datapoint::getValue)).map(Datapoint::getValue).orElse(null);
    }

    @Override
    public Comparator<Double> getComparator() {
        return Comparator.naturalOrder();
    }

    @Override
    public void apply(MetricQueryObject queryObject, Function function) {
    }
}
