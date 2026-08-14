package org.zstack.monitoring.prometheus;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.premium.externalservice.prometheus.Prometheus;
import org.zstack.utils.path.PathUtil;
import static org.zstack.core.Platform.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by xing5 on 2017/6/9.
 */
public class AlertRuleWriter {
    public static class RuleBuilder {
        private String name;
        private String expression;
        private String duration;
        private LinkedHashMap<String, String> labels = new LinkedHashMap<>();
        private LinkedHashMap<String, String> annotations = new LinkedHashMap<>();

        public RuleBuilder name(String v) {
            name = v;
            return this;
        }

        public RuleBuilder expression(String v) {
            expression = v;
            return this;
        }

        public RuleBuilder duration(int v) {
            duration = String.format("%ss", v);
            return this;
        }

        public RuleBuilder label(String key, String value) {
            labels.put(key, value);
            return this;
        }

        public RuleBuilder annotation(String key, String value) {
            annotations.put(key, value);
            return this;
        }

        public String build() {
            assert name != null;
            assert expression != null;
            assert duration != null;

            StringBuilder sb = new StringBuilder(String.format("ALERT %s", name));
            sb.append(String.format(" IF %s", expression));
            sb.append(String.format(" FOR %s", duration));
            if (!labels.isEmpty()) {
                List<String> lst = new ArrayList<>();
                for (Map.Entry<String, String> e : labels.entrySet()) {
                    lst.add(String.format("%s = \"%s\"", e.getKey(), e.getValue()));
                }
                sb.append(String.format(" LABELS { %s }", StringUtils.join(lst, ",")));
            }
            if (!annotations.isEmpty()) {
                List<String> lst = new ArrayList<>();
                for (Map.Entry<String, String> e : annotations.entrySet()) {
                    lst.add(String.format("%s = \"%s\"", e.getKey(), e.getValue()));
                }
                sb.append(String.format(" ANNOTATIONS  { %s }", StringUtils.join(lst, ",")));
            }

            return sb.toString();
        }
    }

    public static RuleBuilder newRuleBuilder() {
        return new RuleBuilder();
    }

    public static final String ALERT_RULES_DIR = PathUtil.join(Prometheus.RULES_ROOT, "alert");
    public static final String ALERT_RULE_PATH = PathUtil.join(ALERT_RULES_DIR, "alert.rule");

    static {
        File ruleRoot = new File(ALERT_RULES_DIR);
        if (!ruleRoot.exists()) {
            ruleRoot.mkdirs();
        }

        File ruleFile = new File(ALERT_RULE_PATH);
        if (!ruleFile.exists()) {
            try {
                if (!ruleFile.createNewFile()) {
                    throw new OperationFailureException(operr("fail to create new File[%s]", ruleFile));
                }
            } catch (IOException e) {
                throw new CloudRuntimeException(e);
            }
        }
    }

    public synchronized static void writeRules(List<RuleBuilder> rbs) {
        File file = new File(ALERT_RULE_PATH);
        try {
            List<String> rules = FileUtils.readLines(file);

            for (RuleBuilder rb : rbs) {
                String rule = rb.build();
                if (rules.contains(rule)) {
                    continue;
                }

                for (String r : rules) {
                    if (r.contains(rb.name)) {
                        throw new OperationFailureException(operr("conflict alert rule[%s], there has been a rule[%s] with the same name", rb.name, r));
                    }
                }

                rules.add(rule);
            }

            FileUtils.writeLines(file, rules);
        } catch (IOException e) {
            throw new CloudRuntimeException(e);
        }
    }

    public synchronized static boolean writeRule(RuleBuilder rb) {
        try {
            String rule = rb.build();
            File file = new File(ALERT_RULE_PATH);
            List<String> rules = FileUtils.readLines(file);
            if (rules.contains(rule)) {
                return false;
            }

            for (String r : rules) {
                if (r.contains(rb.name)) {
                    throw new OperationFailureException(operr("conflict alert rule[%s], there has been a rule[%s] with the same name", rb.name, r));
                }
            }

            rules.add(rule);
            FileUtils.writeLines(file, rules);
            return true;
        } catch (OperationFailureException oe) {
            throw oe;
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
    }

    public synchronized static boolean deleteRules(List<String> names) {
        try {
            File file = new File(ALERT_RULE_PATH);
            List<String> rules = FileUtils.readLines(file);
            boolean ret = rules.removeIf(rule -> {
                for (String name : names) {
                    if (rule.contains(name)) {
                        return true;
                    }
                }

                return false;
            });
            FileUtils.writeLines(file, rules);
            return ret;
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
    }

    public synchronized static boolean deleteRule(String name) {
        try {
            File file = new File(ALERT_RULE_PATH);
            List<String> rules = FileUtils.readLines(file);
            boolean ret = rules.removeIf(rule -> rule.contains(name));
            FileUtils.writeLines(file, rules);
            return ret;
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
    }
}
