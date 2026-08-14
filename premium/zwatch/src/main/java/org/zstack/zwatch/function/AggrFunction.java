package org.zstack.zwatch.function;

import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.zwatch.datatype.Datapoint;
import org.zstack.zwatch.datatype.Function;
import org.zstack.zwatch.datatype.Function.Argument;
import org.zstack.zwatch.datatype.MetricQueryObject;

import java.util.List;

import static org.zstack.core.Platform.argerr;

/**
 * Created by Qi Le on 2021/10/13
 */
public class AggrFunction implements MetricFunction {
    public static final String NAME = "aggr";
    public static final String ARG_OPERATION_NAME = "op";
    public static final String SUM_OPERATION = "sum";
    public static final String AVG_OPERATION = "avg";

    private static final ArgumentChecker ARG_OPERATION = new ArgumentChecker(
            ARG_OPERATION_NAME,
            SUM_OPERATION,
            AVG_OPERATION
    );

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public MetricFunctionPosition getPosition() {
        return MetricFunctionPosition.PRE;
    }

    @Override
    public void checkArguments(List<Argument> arguments) {
        checkNoDuplicateArgument(arguments);
        checkIfMissingArgument(ARG_OPERATION_NAME, arguments);

        arguments.forEach(arg -> {
            if (arg.name.equals(ARG_OPERATION_NAME)) {
                ARG_OPERATION.check(arg.value);
            } else {
                throw new OperationFailureException(argerr("unknown argument[%s]", arg.name));
            }
        });
    }

    @Override
    public List<Datapoint> apply(List<Datapoint> datapoints, Function function) {
        return datapoints;
    }

    @Override
    public void apply(MetricQueryObject queryObject, Function function) {
        Function.Argument argOp = getArgument(ARG_OPERATION_NAME, function.getArguments());
        queryObject.setAggregationOp(argOp.value);
    }
}
