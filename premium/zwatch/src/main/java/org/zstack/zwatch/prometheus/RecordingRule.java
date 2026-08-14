package org.zstack.zwatch.prometheus;

import org.zstack.utils.DebugUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Arrays.asList;

public class RecordingRule {
    private String seriesName;
    private String expression;
    private Map<String, String> labelMapping = new HashMap<>();
    private boolean forLabelMappingOnly;
    private boolean skipRecord = false;

    public static String expressionSpliter = " = ";
    private static final List<String> CALCULATING_SIGNS = asList("(", "+", "-", "*", "/");

    public RecordingRule(boolean forLabelMappingOnly) {
        this.forLabelMappingOnly = forLabelMappingOnly;
    }

    public RecordingRule(String seriesName) {
        this.seriesName = seriesName;
    }

    public void labelMapping(String oldName, String newName) {
        labelMapping.put(oldName, newName);
        labelMapping.put(newName, oldName);
    }

    public boolean isForLabelMappingOnly() {
        return forLabelMappingOnly;
    }

    public void setForLabelMappingOnly(boolean forLabelMappingOnly) {
        this.forLabelMappingOnly = forLabelMappingOnly;
    }

    @Override
    public String toString() {
        DebugUtils.Assert(seriesName != null, "seriesName cannot be null");
        DebugUtils.Assert(expression != null, "expression cannot be null");

        return seriesName + expressionSpliter + expression;
    }

    public String getSeriesName() {
        return seriesName;
    }

    public void setSeriesName(String seriesName) {
        this.seriesName = seriesName;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
        // if expression not include functions, then skip record to prometheus
        Optional opt = CALCULATING_SIGNS.stream().filter(expression::contains).findAny();
        if (!opt.isPresent()) {
            skipRecord = true;
        }
    }

    public Map<String, String> getLabelMapping() {
        return labelMapping;
    }

    public void setLabelMapping(Map<String, String> labelMapping) {
        this.labelMapping = labelMapping;
    }

    public boolean isSkipRecord() {
        return skipRecord;
    }

    public void setSkipRecord(boolean skipRecord) {
        this.skipRecord = skipRecord;
    }

    public String findMappedLabelName(String labelName) {
        return labelMapping.getOrDefault(labelName, labelName);
    }
}
