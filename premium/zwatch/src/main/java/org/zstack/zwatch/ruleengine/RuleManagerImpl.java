package org.zstack.zwatch.ruleengine;

import com.google.common.base.Ticker;
import com.google.common.cache.*;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.Q;
import org.zstack.core.debug.DebugManager;
import org.zstack.core.thread.PeriodicTask;
import org.zstack.core.thread.SyncTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.Component;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.zwatch.ZWatchGlobalConfig;
import org.zstack.zwatch.alarm.AlarmVO;
import org.zstack.zwatch.alarm.AlarmVO_;
import org.zstack.zwatch.datatype.Namespace;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class RuleManagerImpl implements RuleManager, Component {
    private static final CLogger logger = Utils.getLogger(RuleManagerImpl.class);

    @Autowired
    private ThreadFacade thdf;

    public static final String DEBUG_SIGNAL = "ZWatchDumpAlarmRules";

    private Future evaluationTask;
    private Set<Rule> rules = ConcurrentHashMap.newKeySet();
    private Set<RuleEvaluationResultListener> listeners = new HashSet<>();
    private LoadingCache<String, RuleEvaluationResult.RuleEvaluationState> resourceLastStateMap =
                    CacheBuilder.newBuilder()
                            .concurrencyLevel(10)
                            .expireAfterWrite(30L, TimeUnit.MINUTES)
                            .removalListener(new LastStateRemovalListener())
                            .ticker(Ticker.systemTicker())
                            .build(new CacheLoader<String, RuleEvaluationResult.RuleEvaluationState>() {
                                @Override
                                public RuleEvaluationResult.RuleEvaluationState load(String key) throws Exception {
                                    return null;
                                }
                            });

    class LastStateRemovalListener implements RemovalListener<String, RuleEvaluationResult.RuleEvaluationState> {

        @Override
        public void onRemoval(RemovalNotification<String, RuleEvaluationResult.RuleEvaluationState> notification) {
            if (notification.getCause().equals(RemovalCause.EXPIRED)) {
                logger.info(String.format("[%s,%s] has been removed from RuleLastStateMap because %s",
                        notification.getKey(), notification.getValue(), notification.getCause().toString()));
            }
        }
    }

    @Override
    public boolean start() {
        ZWatchGlobalConfig.EVALUATION_INTERVAL.installUpdateExtension((oldConfig, newConfig) -> startEvaluationTask());

        startEvaluationTask();

        DebugManager.registerDebugSignalHandler(DEBUG_SIGNAL, () -> {
            StringBuilder sb = new StringBuilder();
            sb.append("\n================ BEGIN: ZWatch Dump Alarm Rules ===================\n");
            sb.append(String.format("RULE NUMBERS: %d\n", rules.size()));
            for (Rule rule : rules) {
                sb.append(String.format("rule uuid: %s, rule content: %s\n", rule.getUuid(), JSONObjectUtil.toJsonString(rule)));
            }
            sb.append("================ END: ZWatch Dump Alarm Rules =====================\n");
            logger.debug(sb.toString());
        });

        return true;
    }

    private synchronized void startEvaluationTask() {
        if (evaluationTask != null) {
            evaluationTask.cancel(true);
        }

        evaluationTask = thdf.submitPeriodicTask(new PeriodicTask() {
            @Override
            public TimeUnit getTimeUnit() {
                return TimeUnit.SECONDS;
            }

            @Override
            public long getInterval() {
                return ZWatchGlobalConfig.EVALUATION_INTERVAL.value(Long.class);
            }

            @Override
            public String getName() {
                return "zwatch-evaluation-task";
            }

            @Override
            public void run() {
                evaluateRules();
            }
        });
    }

    public void evaluateRules() {
        for (Rule rule : rules) {
            if (!rule.needEvaluate()) {
                continue;
            }

            thdf.syncSubmit(new SyncTask<Void>() {
                private RuleEvaluationResult.RuleEvaluationState evalRuleStates(List<RuleEvaluationResult.RuleEvaluationState> states) {
                    if (states.stream().anyMatch(state -> state.equals(RuleEvaluationResult.RuleEvaluationState.Problem))) {
                        return RuleEvaluationResult.RuleEvaluationState.Problem;
                    } else if (states.stream().anyMatch(state -> state.equals(RuleEvaluationResult.RuleEvaluationState.OK))) {
                        return RuleEvaluationResult.RuleEvaluationState.OK;
                    } else {
                        return RuleEvaluationResult.RuleEvaluationState.NoData;
                    }
                }

                @Override
                public Void call() {
                    List<RuleEvaluationResult> results = rule.eval();
                    AlarmVO vo = Q.New(AlarmVO.class).eq(AlarmVO_.uuid, rule.getUuid()).find();
                    if (vo != null) {
                        Namespace ns = Namespace.getMetricNameSpace(vo.getNamespace(), vo.getMetricName());
                        results = ns.filterRuleEvaluationResults(rule.getUuid(), results);
                    }

                    boolean reactToProblemState = false;

                    RuleEvaluationResult.RuleEvaluationState currentState = evalRuleStates(results.stream()
                            .map(RuleEvaluationResult::getCurrentState)
                            .collect(Collectors.toList()));

                    RuleEvaluationResult.RuleEvaluationState previousState = evalRuleStates(results.stream()
                            .map(RuleEvaluationResult::getLastState)
                            .collect(Collectors.toList()));

                    RuleEvaluationResult evaledResult = new RuleEvaluationResult();
                    evaledResult.setCurrentState(currentState);
                    evaledResult.setLastState(previousState);

                    if (currentState == RuleEvaluationResult.RuleEvaluationState.Problem) {
                        if (rule.getRuleStateChangeListener() != null) {
                            reactToProblemState = rule.getRuleStateChangeListener().needReactToProblemState();
                        }

                        if (reactToProblemState) {
                            if (rule.getRuleStateChangeListener() != null) {
                                rule.getRuleStateChangeListener().problemState(evaledResult, rule);
                            }

                            listeners.forEach(l -> l.problemState(evaledResult, rule));
                        }
                    }

                    // state changed
                    if (evaledResult.isStateChanged()) {
                        if (rule.getRuleStateChangeListener() != null) {
                            rule.getRuleStateChangeListener().stateChanged(evaledResult, rule);
                        }

                        callStateChangeListeners(evaledResult, rule);
                    }

                    // execute action if alarm status is changed from problem to ok
                    List<RuleEvaluationResult> recoveredResults = getRecoveredResults(results, rule);
                    if (CollectionUtils.isNotEmpty(recoveredResults)) {
                        rule.getRuleStateChangeListener().executeActionsFromRecoveredResults(recoveredResults, rule);

                        listeners.forEach(l -> l.executeActionsFromRecoveredResults(recoveredResults, rule));
                    }

                    if (reactToProblemState) {
                        List<RuleEvaluationResult> problemResults = results.stream()
                                .filter(result -> result.getCurrentState().equals(RuleEvaluationResult.RuleEvaluationState.Problem))
                                .collect(Collectors.toList());

                        rule.getRuleStateChangeListener().executeActionsFromProblemResults(problemResults, rule);

                        listeners.forEach(l -> l.executeActionsFromProblemResults(problemResults, rule));
                    }

                    return null;
                }

                private List<RuleEvaluationResult> getRecoveredResults(List<RuleEvaluationResult> results, Rule rule) {
                    return results.stream()
                            .filter( result -> {
                                String identifyLabel = result.getIdentifyLabel();
                                String resourceKey = identifyLabel == null ?
                                        rule.getUuid() : rule.getUuid().concat(result.getIdentifyLabel());

                                if (resourceLastStateMap.getIfPresent(resourceKey) == null) {
                                    resourceLastStateMap.put(resourceKey, result.getCurrentState());
                                    return false;
                                }

                                RuleEvaluationResult.RuleEvaluationState lastState = resourceLastStateMap.getIfPresent(resourceKey);
                                resourceLastStateMap.put(resourceKey, result.getCurrentState());

                                if (result.getCurrentState().equals(RuleEvaluationResult.RuleEvaluationState.OK) &&
                                        lastState.equals(RuleEvaluationResult.RuleEvaluationState.Problem)) {
                                    result.setLastState(lastState);
                                    return true;
                                }

                                return false;
                            })
                            .collect(Collectors.toList());
                }

                @Override
                public String getName() {
                    return getSyncSignature();
                }

                @Override
                public String getSyncSignature() {
                    return "zwatch-rule-evaluation-task";
                }

                @Override
                public int getSyncLevel() {
                    return ZWatchGlobalConfig.EVALUATION_THREADS_NUM.value(Integer.class);
                }
            });
        }
    }

    private void callStateChangeListeners(RuleEvaluationResult ret, Rule rule) {
        for (RuleEvaluationResultListener l : listeners) {
            l.stateChanged(ret, rule);
        }
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public Collection<Rule> getAllRules() {
        return rules;
    }

    @Override
    public void addRules(List<Rule> rules) {
        for (Rule rule : rules)  {
            addRule(rule);
        }
    }

    @Override
    public void addRule(Rule rule) {
        if (logger.isTraceEnabled()) {
            logger.trace(String.format("[ADD RULE]: %s", rule));
        }
        rules.add(rule);
    }

    @Override
    public void deleteRule(String ruleUuid) {
        if (logger.isTraceEnabled()) {
            logger.trace(String.format("[DELETE RULE]: %s", ruleUuid));
        }

        rules.removeIf(r -> r.getUuid().equals(ruleUuid));
    }

    @Override
    public void deleteIf(Predicate<? super Rule> filter) {
        rules.removeIf(filter);
    }

    @Override
    public void clearRules() {
        if (logger.isTraceEnabled()) {
            logger.trace("[CLEAR RULE]: ALL");
        }

        rules.clear();
    }

    @Override
    public void installRuleStateChangeListener(RuleEvaluationResultListener listener) {
        listeners.add(listener);
    }

    @Override
    public LoadingCache getLastStateCachedMap() {
        return resourceLastStateMap;
    }
}
