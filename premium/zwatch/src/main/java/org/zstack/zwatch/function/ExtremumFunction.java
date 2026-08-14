package org.zstack.zwatch.function;

import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.zwatch.datatype.Datapoint;
import org.zstack.zwatch.datatype.Function;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.zstack.core.Platform.argerr;

public abstract class ExtremumFunction implements MetricFunction{
    public static final String ARG_NUM_NAME = "num";
    public static final String ARG_GROUP_BY_NAME = "groupBy";

    public static final ArgumentChecker ARG_NUM = new ArgumentChecker(ARG_NUM_NAME);
    public static final ArgumentChecker ARG_GROUP_BY = new ArgumentChecker(ARG_GROUP_BY_NAME);

    @Override
    public void checkArguments(List<Function.Argument> arguments) {
        boolean match = arguments.stream().anyMatch(it -> !(ARG_NUM.getName().equals(it.name) || ARG_GROUP_BY.getName().equals(it.name)));
        if (match) {
            throw new OperationFailureException(argerr("unknown arguments"));
        }
        checkNoDuplicateArgument(arguments);
        Function.Argument number = getArgument(ARG_NUM.getName(), arguments);
        Function.Argument groupBy = getArgument(ARG_GROUP_BY.getName(), arguments);
        if (number == null && groupBy == null) {
            throw new OperationFailureException(argerr("missing required argument"));
        }
    }

    @Override
    public List<Datapoint> apply(List<Datapoint> datapoints, Function function) {
        if (datapoints.isEmpty()) {
            return datapoints;
        }
        List<Datapoint> ret = new ArrayList<>();
        Function.Argument number = getArgument(ARG_NUM.getName(), function.getArguments());
        Function.Argument groupBy = getArgument(ARG_GROUP_BY.getName(), function.getArguments());
        if (groupBy != null) {
            List<String> finalLabelNames = normalizeFunctionArg(groupBy, ",");
            Map<String, List<Datapoint>> dataPointsGroupByLabelNames = getDataPointsGroupByLabelNames(datapoints, finalLabelNames);

            List<Datapoint> finalRet = ret;
            dataPointsGroupByLabelNames.forEach((key, dps) -> {
                Datapoint dp = dps.get(0);
                Datapoint ndp = new Datapoint();
                ndp.setLabels(new HashMap<>());
                finalLabelNames.forEach(lname -> ndp.getLabels().put(lname, dp.getLabels().get(lname)));
                ndp.setValue(getExtremum(dps));
                finalRet.add(ndp);
            });
        }

        if (number != null) {
            ret = ret.isEmpty() ? datapoints : ret;
            PriorityQueue<Datapoint> q = new PriorityQueue<>(ret.size(),
                    Comparator.comparing(Datapoint::getValue, Comparator.nullsLast(getComparator())));

            q.addAll(ret);
            int num = Integer.parseInt(number.value);
            num = Math.min(num, datapoints.size());
            ret = Stream.generate(q::poll).limit(num).collect(Collectors.toList());
        }

        return ret;
    }

    /**
     * used to processing the list of data points group by labels to get extremum
     * @return return the maximum or minimum value of data points
     */
    public abstract Double getExtremum(List<Datapoint> dps);

    /**
     * used to decide the order to sort data points when using 'num='
     * like top(num=2),low(num=2) etc.
     * @return return the order to sort the data points
     */
    public abstract Comparator<Double> getComparator();

}
