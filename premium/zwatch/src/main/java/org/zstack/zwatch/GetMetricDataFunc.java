package org.zstack.zwatch;

import org.zstack.header.identity.SessionInventory;
import org.zstack.identity.Account;
import org.zstack.identity.ResourceHelper;
import org.zstack.utils.DebugUtils;
import org.zstack.zwatch.datatype.*;
import org.zstack.zwatch.datatype.metric.Metric;
import org.zstack.zwatch.function.MetricFunction;
import org.zstack.zwatch.namespace.AccountFilter;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GetMetricDataFunc {
    public MetricQueryObject queryObject;
    public SessionInventory session;
    public List<Function> functions;

    private List<Datapoint> filterMetricDataByAccount(List<Datapoint> data) {
        if (Account.isAdminPermission(session)) {
            return data;
        }

        AccountFilter filter = AccountFilter.getFilters().get(queryObject.getNamespaceName());
        if (filter == null) {
            return data;
        }

        Namespace ns = Namespace.getMetricNameSpace(queryObject.getNamespaceName(), queryObject.getMetricName());
        Metric metric = ns.getMetrics().stream().filter(m -> m.getName().equals(queryObject.getMetricName())).findFirst().orElse(null);
        if (metric != null && !ns.getEffectiveMetricLabelNames(metric).contains(filter.getLabelName())) {
            return data;
        }

        Collection<String> resourceUuids = ResourceHelper.filterAccessibleResources(
                data.stream().map(d->d.getLabels().get(filter.getLabelName())).collect(Collectors.toSet()),
                session.getAccountUuid());

        return data.stream().filter(d -> resourceUuids.contains(d.getLabels().get(filter.getLabelName()))).collect(Collectors.toList());
    }

    public List<Datapoint> apply() {
        DebugUtils.Assert(queryObject != null, "queryObject cannot be null");
        DebugUtils.Assert(session != null, "session cannot be null");

        if (functions != null) {
            MetricFunction.callPreFunctions(queryObject, functions);
        }

        Namespace ns = Namespace.getMetricNameSpace(queryObject.getNamespaceName(), queryObject.getMetricName());
        List<Datapoint> data = ns.query(queryObject);

        data = filterMetricDataByAccount(data);

        if (functions != null) {
            data = MetricFunction.callFunctions(data, functions);
        }

        if (queryObject.getValueConditions() != null) {
            for (ValueCondition vc : queryObject.getValueConditions()) {
                data = filterByValueCondition(data, vc);
            }
        }

        if (functions != null) {
            data = MetricFunction.callEndFunctions(data, functions);
        }

        return data;
    }

    public Map<Integer, List<Datapoint>> applyReturnWithTotal() {
        DebugUtils.Assert(queryObject != null, "queryObject cannot be null");
        DebugUtils.Assert(session != null, "session cannot be null");

        if (functions != null) {
            MetricFunction.checkFunctions(functions);
            MetricFunction.callPreFunctions(queryObject, functions);
        }

        Namespace ns = Namespace.getMetricNameSpace(queryObject.getNamespaceName(), queryObject.getMetricName());
        List<Datapoint> data = ns.query(queryObject);

        data = filterMetricDataByAccount(data);

        if (functions != null) {
            data = MetricFunction.callFunctions(data, functions);
        }

        if (queryObject.getValueConditions() != null) {
            for (ValueCondition vc : queryObject.getValueConditions()) {
                data = filterByValueCondition(data, vc);
            }
        }

        Integer total = data.size();

        if (functions != null) {
            data = MetricFunction.callEndFunctions(data, functions);
        }

        HashMap<Integer, List<Datapoint>> map = new HashMap<>();
        map.put(total, data);
        return map;
    }

    public Map<Integer,List<Datapoint>> applyWithMissingData(String restrictLabelName, List<String> uuids, long endTime) {
        DebugUtils.Assert(queryObject != null, "queryObject cannot be null");
        DebugUtils.Assert(session != null, "session cannot be null");

        if (functions != null) {
            MetricFunction.checkFunctions(functions);
            MetricFunction.callPreFunctions(queryObject, functions);
        }

        Namespace ns = Namespace.getMetricNameSpace(queryObject.getNamespaceName(), queryObject.getMetricName());
        List<Datapoint> data = ns.query(queryObject);

        if (restrictLabelName != null) {
            List<String> existsUuids = data.stream().map(dp -> dp.getLabels().get(restrictLabelName)).distinct().collect(Collectors.toList());
            uuids.removeAll(existsUuids);
            for (String uuid : uuids) {
                Datapoint dp = new Datapoint();
                dp.setLabels(new HashMap<>());
                dp.getLabels().put(restrictLabelName, uuid);
                dp.setTime(endTime);
                dp.setValue(null);
                data.add(dp);
            }
        }

        data = filterMetricDataByAccount(data);

        if (functions != null) {
            data = MetricFunction.callFunctions(data, functions);
        }

        if (queryObject.getValueConditions() != null) {
            for (ValueCondition vc : queryObject.getValueConditions()) {
                data = filterByValueCondition(data, vc);
            }
        }

        Integer total = data.size();

        if (functions != null) {
            data = MetricFunction.callEndFunctions(data, functions);
        }

        HashMap<Integer, List<Datapoint>> map = new HashMap<>();
        map.put(total, data);
        return map;
    }

    private List<Datapoint> filterByValueCondition(List<Datapoint> data, ValueCondition q) {
        if (q.getOp() == ValueCondition.Operator.Equal) {
            return data.stream().filter(dp -> dp.getValue() != null && dp.getValue().equals(Double.valueOf(q.getValue()))).collect(Collectors.toList());
        } else if (q.getOp() == ValueCondition.Operator.NotEqual) {
            return data.stream().filter(dp -> dp.getValue() != null && !dp.getValue().equals(Double.valueOf(q.getValue()))).collect(Collectors.toList());
        } else if (q.getOp() == ValueCondition.Operator.GreaterThan) {
            return data.stream().filter(dp -> dp.getValue() != null && dp.getValue() > Double.parseDouble(q.getValue())).collect(Collectors.toList());
        } else if (q.getOp() == ValueCondition.Operator.GreaterOrEqual) {
            return data.stream().filter(dp -> dp.getValue() != null && dp.getValue() >= Double.parseDouble(q.getValue())).collect(Collectors.toList());
        } else if (q.getOp() == ValueCondition.Operator.LessThan) {
            return data.stream().filter(dp -> dp.getValue() == null || dp.getValue() < Double.parseDouble(q.getValue())).collect(Collectors.toList());
        } else if (q.getOp() == ValueCondition.Operator.LessOrEqual) {
            return data.stream().filter(dp -> dp.getValue() == null || dp.getValue() <= Double.parseDouble(q.getValue())).collect(Collectors.toList());
        } else {
            return data;
        }
    }
}
