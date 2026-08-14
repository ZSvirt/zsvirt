package org.zstack.zwatch.ruleengine;

import org.zstack.zwatch.datatype.Datapoint;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;

/**
 * Created by kayo on 2018/9/17.
 */
public class AverageAlgorithm extends MetricRule.DefaultAlgorithm {
    @Override
    public List<RuleEvaluationResult> eval(List<Datapoint> data, MetricRule rule) {
        List<Datapoint> dps = new ArrayList<>();
        OptionalDouble optional = data.stream().map(Datapoint::getValue).mapToDouble(a -> a)
                .average();

        if (!optional.isPresent()) {
            return super.eval(dps, rule);
        }

        Double averages = optional.getAsDouble();

        Datapoint datapoint = new Datapoint();
        datapoint.setValue(averages);
        datapoint.setTime(System.currentTimeMillis());
        dps.add(datapoint);

        return super.eval(dps, rule);
    }
}
