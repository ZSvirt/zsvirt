package org.zstack.zwatch.function;

import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.zwatch.datatype.Datapoint;
import org.zstack.zwatch.datatype.Function;
import org.zstack.zwatch.datatype.MetricQueryObject;

import java.util.List;

import static org.zstack.core.Platform.argerr;

public class LimitFunction implements MetricFunction {
    public static final String NAME = "limit";

    public static final ArgumentChecker ARG_LIMIT = new ArgumentChecker("limit", (v) -> {
        try {
            if (Integer.valueOf(v) < 0) {
                throw new OperationFailureException(argerr("invalid argument[limit:%s], it can't be a negative number", v));
            }
        } catch (NumberFormatException e) {
            throw new OperationFailureException(argerr("value[%s] is not a Integer number", v));
        }
    });

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void checkArguments(List<Function.Argument> arguments) {
        checkIfMissingArgument(ARG_LIMIT.getName(), arguments);
        checkNoDuplicateArgument(arguments);
        arguments.forEach(a -> ARG_LIMIT.check(a.value));
    }

    @Override
    public List<Datapoint> apply(List<Datapoint> datapoints, Function function) {
        Function.Argument arg = getArgument(ARG_LIMIT.getName(), function.getArguments());
        int limit = Integer.parseInt(arg.value);
        if (limit >= datapoints.size()) {
            return datapoints;
        } else {
            return datapoints.subList(0, limit);
        }
    }

    @Override
    public void apply(MetricQueryObject queryObject, Function function) {
    }
}
