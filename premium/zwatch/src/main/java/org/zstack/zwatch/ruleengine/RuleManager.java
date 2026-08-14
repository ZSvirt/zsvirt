package org.zstack.zwatch.ruleengine;

import com.google.common.cache.LoadingCache;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

public interface RuleManager {
    Collection<Rule> getAllRules();

    void addRules(List<Rule> rules);

    void addRule(Rule rule);

    void deleteRule(String ruleUuid);

    // the function has a prototype of
    // boolean filter(Rule rule)
    // returns true to if the rule is intended to be deleted
    void deleteIf(Predicate<? super Rule> filter);

    void clearRules();

    void installRuleStateChangeListener(RuleEvaluationResultListener listener);

    LoadingCache getLastStateCachedMap();
}
