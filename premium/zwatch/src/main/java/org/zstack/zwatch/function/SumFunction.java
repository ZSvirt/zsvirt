package org.zstack.zwatch.function;

import org.zstack.zwatch.datatype.Datapoint;
import org.zstack.zwatch.datatype.Function;
import org.zstack.zwatch.datatype.MetricQueryObject;

import java.util.ArrayList;
import java.util.List;

public class SumFunction implements MetricFunction {
    public static final String NAME = "sum";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void checkArguments(List<Function.Argument> arguments) {
    }

    @Override
    public List<Datapoint> apply(List<Datapoint> datapoints, Function function) {
        double value = 0;
        for (Datapoint datapoint : datapoints) {
            value += datapoint.getValue();
        }

        Datapoint dp = new Datapoint();
        dp.setValue(value);

        List<Datapoint> ret = new ArrayList<>();
        ret.add(dp);
        return ret;
    }

    @Override
    public void apply(MetricQueryObject queryObject, Function function) {
    }
}
