package org.zstack.zwatch.function;

import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.zwatch.datatype.Datapoint;
import org.zstack.zwatch.datatype.Function;
import org.zstack.zwatch.datatype.MetricQueryObject;

import java.util.Comparator;
import java.util.List;

import static org.zstack.core.Platform.argerr;

public class SortFunction implements MetricFunction {
    public static final String NAME = "sort";
    public static final String ARG_BY_NAME = "by";
    public static final String ARG_DIRECTION_NAME = "direction";
    public static final String BY_VALUE = "value";
    public static final String BY_TIME = "time";
    public static final String DIRECTION_ASC = "asc";
    public static final String DIRECTION_DESC = "desc";

    public static final ArgumentChecker ARG_BY = new ArgumentChecker(ARG_BY_NAME, BY_VALUE, BY_TIME);
    public static final ArgumentChecker ARG_DIRECTION = new ArgumentChecker(ARG_DIRECTION_NAME, DIRECTION_ASC, DIRECTION_DESC);

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void checkArguments(List<Function.Argument> arguments) {
        checkNoDuplicateArgument(arguments);
        checkIfMissingArgument(ARG_BY.getName(), arguments);
        checkIfMissingArgument(ARG_DIRECTION.getName(), arguments);

        arguments.forEach(arg -> {
            if (arg.name.equals(ARG_BY.getName())) {
                ARG_BY.check(arg.value);
            } else if (arg.name.equals(ARG_DIRECTION.getName())) {
                ARG_DIRECTION.check(arg.value);
            } else {
                throw new OperationFailureException(argerr("unknown argument[%s]", arg.name));
            }
        });
    }

    @Override
    public List<Datapoint> apply(List<Datapoint> datapoints, Function function) {
        Function.Argument argBy = getArgument(ARG_BY.getName(), function.getArguments());
        Function.Argument argDir = getArgument(ARG_DIRECTION.getName(), function.getArguments());
        boolean asc = argDir.value.equals(DIRECTION_ASC);
        if (argBy.value.equals(BY_VALUE)) {
            if (asc) {
                datapoints.sort(Comparator.comparing(Datapoint::getValue, Comparator.nullsLast(Comparator.naturalOrder())));
            } else {
                datapoints.sort(Comparator.comparing(Datapoint::getValue, Comparator.nullsLast(Comparator.reverseOrder())));
            }
        } else {
            if (asc) {
                datapoints.sort(Comparator.comparingLong(Datapoint::getTime));
            } else {
                datapoints.sort(Comparator.comparingLong(Datapoint::getTime).reversed());
            }
        }

        return datapoints;
    }

    @Override
    public void apply(MetricQueryObject queryObject, Function function) {
    }
}
