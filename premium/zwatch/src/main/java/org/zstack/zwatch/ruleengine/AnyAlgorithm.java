package org.zstack.zwatch.ruleengine;

import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.zwatch.datatype.Datapoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by kayo on 2018/9/18.
 */
public class AnyAlgorithm extends MetricRule.DefaultAlgorithm {
    protected static final CLogger logger = Utils.getLogger(AnyAlgorithm.class);

    @Override
    public List<RuleEvaluationResult> eval(List<Datapoint> data, MetricRule rule) {
        Map<String, List<Datapoint>> groups = groupByLabels(data);

        if (logger.isTraceEnabled()) {
            logger.trace(String.format("dump grouped data points:\n%s",
                    JSONObjectUtil.dumpPretty(groups)));
        }

        List<RuleEvaluationResult> results = new ArrayList<>();

        // if no labels exists, just evaluate the whole data set
        if (groups.isEmpty()) {
            RuleEvaluationResult singleResult = super.eval(data, rule).get(0);

            results.add(singleResult);
            return results;
        }

        for (Map.Entry<String, List<Datapoint>> entry : groups.entrySet()) {
            RuleEvaluationResult singleResult = super.eval(entry.getValue(), rule).get(0);

            singleResult.setIdentifyLabel(entry.getKey());
            results.add(singleResult);
        }

        if (logger.isTraceEnabled()) {
            logger.trace(String.format("dump results of alarm any algorithm:\n%s",
                    JSONObjectUtil.dumpPretty(results)));
        }

        return results;
    }

    private Map<String, List<Datapoint>> groupByLabels(List<Datapoint> datapoints) {
        List<String> finalLabelNames = datapoints.get(0).getLabels().keySet().stream().map(String::trim).sorted().collect(Collectors.toList());
        Map<String, List<Datapoint>> groups = new HashMap<>();
        datapoints.forEach(dp -> {
            StringBuilder sb = new StringBuilder();
            finalLabelNames.forEach(labelName -> {
                if (dp.getLabels() == null) {
                    return;
                }

                String value = String.valueOf(dp.getLabels().get(labelName));
                if (value == null) {
                    return;
                }

                sb.append(String.format("::%s:%s::", labelName, value));
            });

            String key = sb.toString();
            if (key.isEmpty()) {
                return;
            }

            List<Datapoint> dps = groups.computeIfAbsent(key, x->new ArrayList<>());
            dps.add(dp);
        });

        return groups;
    }
}
