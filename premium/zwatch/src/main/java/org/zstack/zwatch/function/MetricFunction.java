package org.zstack.zwatch.function;

import org.zstack.header.core.StaticInit;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.BeanUtils;
import org.zstack.zwatch.datatype.Datapoint;
import org.zstack.zwatch.datatype.Function;
import org.zstack.zwatch.datatype.MetricQueryObject;

import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.argerr;

public interface MetricFunction {
    enum MetricFunctionPosition {
        PRE,
        FRONT,
        END
    }

    String getName();

    default MetricFunctionPosition getPosition() {
        return MetricFunctionPosition.FRONT;
    }

    Map<String, MetricFunction> preFunctions = new HashMap<>();
    Map<String, MetricFunction> frontFunctions = new HashMap<>();
    Map<String, MetricFunction> endFunctions = new HashMap<>();

    @StaticInit
    static void staticInit() {
        BeanUtils.reflections.getSubTypesOf(MetricFunction.class).forEach(clz -> {
            if (Modifier.isAbstract(clz.getModifiers())) {
                return;
            }
            try {
                MetricFunction func = clz.getConstructor().newInstance();
                if (func.getPosition() == MetricFunctionPosition.PRE) {
                    preFunctions.put(func.getName(), func);
                } else if (func.getPosition() == MetricFunctionPosition.FRONT) {
                    frontFunctions.put(func.getName(), func);
                } else {
                    endFunctions.put(func.getName(), func);
                }
            } catch (Exception e) {
                throw new CloudRuntimeException(e);
            }
        });
    }

    void checkArguments(List<Function.Argument> arguments);

    /**
     * Apply the function to the data point list, which is the result of
     * this query or the result of some previous functions.
     * Intuitively, this method is called after the database query finished.
     * More specifically, this method is used by FRONT and END functions.
     *
     * @param datapoints the list of Datapoints to be processed by the function.
     * @param function the function to apply actions to the list of Datapoints
     * @return the processed list of Datapoints
     */
    List<Datapoint> apply(List<Datapoint> datapoints, Function function);

    /**
     * Apply the function to the MetricQueryObject, which contains query params
     * and more information about database query.
     * This method is used by PRE functions, and is called before the database query.
     * Usually, this function will add or modify some params in the query object.
     *
     * @param queryObject MetricQueryObject contains database query params and information
     * @param function the function to apply actions to the query object.
     */
    void apply(MetricQueryObject queryObject, Function function);

    default Function.Argument getArgument(String name, List<Function.Argument> args) {
        Optional<Function.Argument> opt = args.stream().filter(a->a.name.equals(name)).findAny();
        return opt.orElse(null);
    }

    default void checkIfMissingArgument(String name, List<Function.Argument> args) {
        Function.Argument arg = getArgument(name, args);
        if (arg == null) {
            throw new OperationFailureException(argerr("missing required argument[%s]", name));
        }
    }

    default void checkNoDuplicateArgument(List<Function.Argument> args) {
        Map<String, Integer> count = new HashMap<>();
        args.forEach(a -> {
            int c = count.computeIfAbsent(a.name, x->0);
            count.put(a.name, ++c);
        });

        count.forEach((k, v)-> {
            if (v > 1) {
                throw new OperationFailureException(argerr("duplicate argument[%s]", k));
            }
        });
    }

    static void checkFunctions(List<Function> funcs) {
        funcs.forEach(func -> {
            MetricFunction mfunc = frontFunctions.get(func.getName());
            if (mfunc == null) {
                mfunc = endFunctions.get(func.getName());
            }
            if (mfunc == null) {
                mfunc = preFunctions.get(func.getName());
            }
            if (mfunc == null) {
                throw new OperationFailureException(argerr("unknown function[%s]", func.getName()));
            }

            mfunc.checkArguments(func.getArguments());
        });
    }

    static void callPreFunctions(MetricQueryObject queryObject, List<Function> funcs) {
        for (Function func : funcs) {
            MetricFunction mfunc = preFunctions.get(func.getName());

            if (mfunc == null) {
                continue;
            }

            mfunc.apply(queryObject, func);
        }
    }

    static List<Datapoint> callFunctions(List<Datapoint> datapoints, List<Function> funcs) {
        for (Function func : funcs) {
            MetricFunction mfunc = frontFunctions.get(func.getName());

            if (mfunc == null) {
                continue;
            }

            datapoints = mfunc.apply(datapoints, func);
        }

        return datapoints;
    }

    static List<Datapoint> callEndFunctions(List<Datapoint> datapoints, List<Function> funcs) {
        for (Function func : funcs) {
            MetricFunction mfunc = endFunctions.get(func.getName());

            if (mfunc == null) {
                continue;
            }

            datapoints = mfunc.apply(datapoints, func);
        }

        return datapoints;
    }

    default Map<String, List<Datapoint>> getDataPointsGroupByLabelNames(List<Datapoint> datapoints, List<String> labelNames) {
        Map<String, List<Datapoint>> labelNamesDataPoints = new HashMap<>();
        datapoints.forEach(dp -> {
            StringBuilder sb = new StringBuilder();
            labelNames.forEach(labelName -> {
                if (dp.getLabels() == null) {
                    return;
                }

                String value = dp.getLabels().get(labelName);
                if (value == null) {
                    return;
                }

                sb.append(String.format("::%s:%s::", labelName, value));
            });

            String key = sb.toString();
            if (key.isEmpty()) {
                return;
            }

            List<Datapoint> dps = labelNamesDataPoints.computeIfAbsent(key, x->new ArrayList<>());
            dps.add(dp);
        });
        return labelNamesDataPoints;
    }

    /**
     * normalize the arguments of function, get the label list from arguments
     * like average(groupBy="label1,label2") etc.
     */
    default List<String> normalizeFunctionArg(Function.Argument argument, String regex) {
        List<String> labelNames = new ArrayList<>();
        Collections.addAll(labelNames, argument.value.split(regex));
        labelNames = labelNames.stream().map(String::trim).collect(Collectors.toList());
        return labelNames;
    }
}
