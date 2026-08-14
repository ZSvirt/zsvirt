package org.zstack.zwatch.function;

import org.zstack.zwatch.datatype.Datapoint;
import org.zstack.zwatch.datatype.Function;
import org.zstack.zwatch.datatype.MetricQueryObject;

import java.util.*;
import java.util.stream.Collectors;

public class AverageFunction implements MetricFunction {
    public static final String NAME = "average";
    public static final String ARG_GROUP_BY_NAME = "groupBy";

    public static ArgumentChecker ARG_GROUP_BY = new ArgumentChecker(ARG_GROUP_BY_NAME);

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void checkArguments(List<Function.Argument> arguments) {
        checkIfMissingArgument(ARG_GROUP_BY.getName(), arguments);
    }

    @Override
    public List<Datapoint> apply(List<Datapoint> datapoints, Function function) {
        Function.Argument groupBy = getArgument(ARG_GROUP_BY.getName(), function.getArguments());
        List<String> finalLabelNames = normalizeFunctionArg(groupBy, ",");

        Map<String, List<Datapoint>> averages = getDataPointsGroupByLabelNames(datapoints, finalLabelNames);
        List<Datapoint> ret = new ArrayList<>();
        averages.forEach((key, dps) -> {
            Datapoint dp = dps.get(0);

            Datapoint ndp = new Datapoint();
            ndp.setLabels(new HashMap<>());
            finalLabelNames.forEach(lname -> ndp.getLabels().put(lname, dp.getLabels().get(lname)));

            double sum = 0;

            if (dps.size() == 1 && dps.get(0).getValue() == null) {
                ndp.setValue(null);
            } else {
                for (Datapoint datapoint : dps) {
                    sum += datapoint.getValue();
                }

                ndp.setValue(sum / dps.size());
            }

            ret.add(ndp);
        });

        return ret;
    }

    @Override
    public void apply(MetricQueryObject queryObject, Function function) {
    }
}
