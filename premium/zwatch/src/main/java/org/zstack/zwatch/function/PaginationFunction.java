package org.zstack.zwatch.function;

import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.zwatch.datatype.Datapoint;
import org.zstack.zwatch.datatype.Function;
import org.zstack.zwatch.datatype.MetricQueryObject;

import java.util.ArrayList;
import java.util.List;

import static org.zstack.core.Platform.argerr;

public class PaginationFunction implements MetricFunction {
    public static final String NAME = "pagination";

    public static final ArgumentChecker ARG_LIMIT = new ArgumentChecker("limit", (v)-> {
        try {
            if (Integer.parseInt(v) < 0) {
                throw new OperationFailureException(argerr("invalid argument[limit:%s], it can't be a negative number", v));
            }
        } catch (NumberFormatException e) {
            throw new OperationFailureException(argerr("value[%s] is not a Integer number", v));
        }
    });

    public static final ArgumentChecker ARG_START = new ArgumentChecker("start", (v)-> {
        try {
            if (Integer.parseInt(v) < 0) {
                throw new OperationFailureException(argerr("invalid argument[start:%s], it can't be a negative number", v));
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
    public MetricFunctionPosition getPosition() {
        return MetricFunctionPosition.END;
    }

    @Override
    public void checkArguments(List<Function.Argument> arguments) {
        checkIfMissingArgument(ARG_LIMIT.getName(), arguments);
        checkIfMissingArgument(ARG_START.getName(), arguments);
        checkNoDuplicateArgument(arguments);
        arguments.forEach(arg -> {
            if (arg.name.equals(ARG_LIMIT.getName())) {
                ARG_LIMIT.check(arg.value);
            } else if (arg.name.equals(ARG_START.getName())) {
                ARG_START.check(arg.value);
            } else {
                throw new OperationFailureException(argerr("unknown argument[%s]", arg.name));
            }
        });
    }

    @Override
    public List<Datapoint> apply(List<Datapoint> datapoints, Function function) {
        Function.Argument argLimit = getArgument(ARG_LIMIT.getName(), function.getArguments());
        Function.Argument argStart = getArgument(ARG_START.getName(), function.getArguments());
        int limit = Integer.parseInt(argLimit.value);
        int start = Integer.parseInt(argStart.value);
        if (limit >= datapoints.size()) {
            return datapoints;
        } else {
            if (start > datapoints.size()) {
                return new ArrayList<>();
            }

            return datapoints.subList(start, start + limit);
        }
    }

    @Override
    public void apply(MetricQueryObject queryObject, Function function) {
    }
}
