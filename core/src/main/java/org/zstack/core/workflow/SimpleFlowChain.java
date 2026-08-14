package org.zstack.core.workflow;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.core.AsyncBackup;
import org.zstack.header.core.progress.ProgressFlowChainProcessorFactory;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.FieldUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.lang.reflect.Field;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.inerr;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 2:38 PM
 * To change this template use File | Settings | File Templates.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class SimpleFlowChain implements FlowTrigger, FlowRollback, FlowChain, FlowChainMutable {
    private static final CLogger logger = Utils.getLogger(SimpleFlowChain.class);

    private String id;
    private List<Flow> flows = new ArrayList<>();
    private final Stack<Flow> rollBackFlows = new Stack<>();
    private Map<Object, Object> data = new HashMap<>();
    private int currentLoop = 0;
    private Iterator<Flow> it;
    private boolean isStart = false;
    private boolean isRollbackStart = false;
    private Flow currentFlow;
    private Flow currentRollbackFlow;
    private ErrorCode errorCode;
    private FlowContextHandler contextHandler;
    private FlowErrorHandler errorHandler;
    private FlowDoneHandler doneHandler;
    private FlowFinallyHandler finallyHandler;
    private String name;
    private boolean skipRestRollbacks;
    private boolean allowEmptyFlow;
    private boolean enableDebugLog = true;
    private FlowMarshaller flowMarshaller;
    private List<FlowChainProcessor> processors;
    private Function<Map, ErrorCode> preCheck;
    private List<List<Runnable>> afterDone = new ArrayList<>();
    private List<List<Runnable>> afterError = new ArrayList<>();
    private List<List<Runnable>> afterFinal = new ArrayList<>();
    private List<AsyncBackup> asyncBackups = new ArrayList<>();

    @Autowired(required = false)
    ProgressFlowChainProcessorFactory progressFactory;

    private boolean isFailCalled;

    private static final Map<String, WorkFlowStatistic> statistics = new ConcurrentHashMap<>();

    private class FlowStopWatch {
        Map<String, Long> beginTime = new HashMap<>();
        void start(Flow flow) {
            long btime = System.currentTimeMillis();
            if (currentFlow != null) {
                String cname = getFlowNameWithoutLocation(currentFlow);
                long stime = beginTime.get(cname);
                WorkFlowStatistic stat = statistics.get(cname);
                stat.addStatistic(btime - stime);

                printDebugLog(String.format("[FlowChain(%s):%s, flow:%s] takes %sms to complete",
                        id, name, cname, stat.getTotalTime()));
            }

            String fname = getFlowNameWithoutLocation(flow);
            beginTime.put(fname, btime);
            WorkFlowStatistic stat = statistics.get(fname);
            if (stat == null) {
                stat = new WorkFlowStatistic();
                stat.setName(fname);
                statistics.put(fname, stat);
            }
        }

        void stop() {
            long etime = System.currentTimeMillis();
            if (currentFlow != null) {
                String fname = getFlowNameWithoutLocation(currentFlow);
                long stime = beginTime.get(fname);
                WorkFlowStatistic stat = statistics.get(fname);
                stat.addStatistic(etime - stime);
            }
        }
    }

    private FlowStopWatch stopWatch;
    private boolean allowWatch = false;
    public void allowWatch() {
        this.allowWatch = true;
        if (stopWatch == null) {
            stopWatch = new FlowStopWatch();
        }
    }

    public void disableDebugLog() {
        this.enableDebugLog = false;
    }

    {
        if (CoreGlobalProperty.PROFILER_WORKFLOW) {
            stopWatch = new FlowStopWatch();
        }
    }

    @Autowired
    private ErrorFacade errf;

    public SimpleFlowChain() {
        id = "FCID_" + Platform.getUuid().substring(0, 8);
    }

    public SimpleFlowChain(String chainName) {
        this();
        this.name = chainName;
    }

    public SimpleFlowChain(Map<String, Object> data) {
        id = "FCID_" + Platform.getUuid().substring(0, 8);
        this.data.putAll(data);
    }

    public static SimpleFlowChain of(String chainName) {
        return new SimpleFlowChain(chainName);
    }

    @Override
    public List<Flow> getFlows() {
        return flows;
    }

    @Override
    public void setFlows(List<Flow> flows) {
        this.flows = flows;
    }

    @Override
    public FlowDoneHandler getFlowDoneHandler() {
        return doneHandler;
    }

    @Override
    public void setFlowDoneHandler(FlowDoneHandler handler) {
        done(handler);
    }

    @Override
    public FlowErrorHandler getFlowErrorHandler() {
        return errorHandler;
    }

    @Override
    public void setFlowErrorHandler(FlowErrorHandler handler) {
        error(handler);
    }

    @Override
    public FlowFinallyHandler getFlowFinallyHandler() {
        return finallyHandler;
    }

    @Override
    public void setFlowFinallyHandler(FlowFinallyHandler handler) {
        Finally(handler);
    }

    @Override
    public String getChainName() {
        return name;
    }

    @Override
    public void setChainName(String name) {
        setName(name);
    }

    @Override
    public Map getChainData() {
        return data;
    }

    @Override
    public void setChainData(Map data) {
        setData(data);
    }

    @Override
    public FlowChain insert(Flow flow) {
        flows.add(0, flow);
        return this;
    }

    @Override
    public FlowChain insert(int pos, Flow flow) {
        flows.add(pos, flow);
        return this;
    }

    @Override
    public FlowChain setFlowMarshaller(FlowMarshaller marshaller) {
        flowMarshaller = marshaller;
        return this;
    }

    @Override
    public SimpleFlowChain enableProgressReport() {
        if (progressFactory != null) {
            FlowChainProcessor progressProcessor = progressFactory.create();
            addProcessor(progressProcessor);
        }
        return this;
    }

    public SimpleFlowChain then(Flow flow) {
        flows.add(flow);
        return this;
    }

    public SimpleFlowChain then(String flowName, Consumer<FlowTrigger> consumer) {
        return then(Flow.of(flowName)
                .handle(consumer)
                .build());
    }

    public SimpleFlowChain ctxHandler(FlowContextHandler handler) {
        DebugUtils.Assert(contextHandler==null, "there has been an FlowContextHandler installed");
        contextHandler = handler;
        return this;
    }

    public SimpleFlowChain error(FlowErrorHandler handler) {
        DebugUtils.Assert(errorHandler==null, "there has been an FlowErrorHandler installed");
        errorHandler = handler;
        return this;
    }

    @SuppressWarnings("rawtypes")
    public SimpleFlowChain error(Consumer<ErrorCode> handler) {
        DebugUtils.Assert(!asyncBackups.isEmpty(),
                "propagateExceptionTo() must be called before error(Consumer<ErrorCode>)."
                        + " Call .propagateExceptionTo(completion) or .propagateExceptionTo((AsyncBackup) null) if you don't need async backup");
        AsyncBackup firstAsyncBackup = this.asyncBackups.get(0);
        AsyncBackup[] otherAsyncBackups = this.asyncBackups.subList(1, this.asyncBackups.size()).toArray(new AsyncBackup[0]);

        DebugUtils.Assert(handler != null, "handler of errorHandler should not be null");
        return error(new FlowErrorHandler(firstAsyncBackup, otherAsyncBackups) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                handler.accept(errCode);
            }
        });
    }

    @Override
    public FlowChain Finally(FlowFinallyHandler handler) {
        finallyHandler = handler;
        return this;
    }

    @Override
    public FlowChain setData(Map data) {
        this.data.putAll(data);
        return this;
    }

    @Override
    public FlowChain putData(Entry...es) {
        for (Map.Entry e : es) {
            data.put(e.getKey(), e.getValue());
        }
        return this;
    }

    @Override
    public FlowChain setName(String name) {
        this.name = name;
        return this;
    }

    @Override
    public FlowChain preCheck(java.util.function.Function<Map, ErrorCode> checker) {
        this.preCheck = checker;
        return this;
    }

    @Override
    public void addProcessor(FlowChainProcessor processor) {
        if (processors == null) {
            processors = new ArrayList<>();
        }
        processors.add(processor);
    }

    @Override
    public Map getData() {
        return this.data;
    }

    public SimpleFlowChain propagateExceptionTo(AsyncBackup... backups) {
        DebugUtils.Assert(backups != null, "backups in methods propagateExceptionTo() must be not null");
        DebugUtils.Assert(doneHandler == null, "propagateExceptionTo() must be called before SimpleFlowChain.done()");
        DebugUtils.Assert(errorHandler == null, "propagateExceptionTo() must be called before SimpleFlowChain.error()");
        DebugUtils.Assert(finallyHandler == null, "propagateExceptionTo() must be called before SimpleFlowChain.Finally()");

        this.asyncBackups.addAll(Arrays.asList(backups));
        this.asyncBackups.removeIf(Objects::isNull);

        if (this.asyncBackups.isEmpty()) {
            this.asyncBackups.add(null);
        }
        return this;
    }

    @Override
    public SimpleFlowChain done(FlowDoneHandler handler) {
        DebugUtils.Assert(doneHandler==null, "there has been a FlowDoneHandler installed");
        doneHandler = handler;
        return this;
    }

    @SuppressWarnings("rawtypes")
    public SimpleFlowChain done(Runnable runnable) {
        DebugUtils.Assert(!asyncBackups.isEmpty(),
                "propagateExceptionTo() must be called before done(Runnable)."
                        + " Call .propagateExceptionTo(completion) or .propagateExceptionTo((AsyncBackup) null) if you don't need async backup");
        AsyncBackup firstAsyncBackup = this.asyncBackups.get(0);
        AsyncBackup[] otherAsyncBackups = this.asyncBackups.subList(1, this.asyncBackups.size()).toArray(new AsyncBackup[0]);

        DebugUtils.Assert(runnable != null, "runnable of doneHandler should not be null");
        return done(new FlowDoneHandler(firstAsyncBackup, otherAsyncBackups) {
            @Override
            public void handle(Map data) {
                runnable.run();
            }
        });
    }

    private void collectAfterRunnable(Flow flow) {
        List<Field> ad = FieldUtils.getAnnotatedFieldsOnThisClass(AfterDone.class, flow.getClass());
        for (Field f : ad) {
            List lst = FieldUtils.getFieldValue(f.getName(), flow);
            if (lst != null) {
                afterDone.add(lst);
            }
        }

        ad = FieldUtils.getAnnotatedFieldsOnThisClass(AfterError.class, flow.getClass());
        for (Field f : ad) {
            List lst = FieldUtils.getFieldValue(f.getName(), flow);
            if (lst != null) {
                afterError.add(lst);
            }
        }

        ad = FieldUtils.getAnnotatedFieldsOnThisClass(AfterFinal.class, flow.getClass());
        for (Field f : ad) {
            List lst = FieldUtils.getFieldValue(f.getName(), flow);
            if (lst != null) {
                afterFinal.add(lst);
            }
        }
    }

    private void printDebugLog(String msg) {
        if (enableDebugLog) {
            logger.debug(msg);
        }
    }

    private void runFlow(Flow flow) {
        try {
            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                String flowName = null;
                String flowClassName = null;
                if (currentFlow != null) {
                    flowName = getFlowName(currentFlow);
                    flowClassName = currentFlow.getClass().getName();
                }

                throw new CloudRuntimeException(String.format("flow[%s:%s] opened a transaction but forgot closing it", flowClassName, flowName));
            }

            Flow toRun = null;
            if (flowMarshaller != null) {
                toRun = flowMarshaller.marshalTheNextFlow(currentFlow == null ? null : currentFlow.getClass().getName(),
                        flow.getClass().getName(), this, data);
                if (toRun != null) {
                    printDebugLog(String.format("[FlowChain(%s): %s] FlowMarshaller[%s] replaces the next flow[%s] to the flow[%s]",
                            id, name, flowMarshaller.getClass(), flow.getClass(), toRun.getClass()));
                }
            }

            if (toRun == null) {
                toRun = flow;
            }

            if (CoreGlobalProperty.PROFILER_WORKFLOW || allowWatch) {
                stopWatch.start(toRun);
            }

            currentFlow = toRun;

            String flowName = getFlowName(currentFlow);
            String info = String.format("[FlowChain(%s): %s] start executing flow[%s]", id, name, flowName);
            printDebugLog(info);

            collectAfterRunnable(toRun);

            if (preCheck != null) {
                printDebugLog(String.format("[FlowChain(%s): %s] start executing pre-check for flow[%s]", id, name, flowName));

                ErrorCode err = preCheck.apply(data);
                if (err != null) {
                    this.fail(err);
                    return;
                }
            }

            if (isSkipFlow(toRun)) {
                printDebugLog(String.format("[FlowChain(%s): %s] skip flow[%s]", id, name, flowName));
                rollBackFlows.remove(toRun);
                this.next();
            } else {
                if (contextHandler != null) {
                    contextHandler.saveContext(toRun);
                    if (contextHandler.cancelled()) {
                        this.fail(contextHandler.getCancelError());
                        return;
                    }
                }
                toRun.run(this, data);
            }
        } catch (OperationFailureException oe) {
            String errInfo = oe.getErrorCode() != null ? oe.getErrorCode().toString() : "";
            logger.warn(errInfo, oe);
            fail(oe.getErrorCode());
        } catch (FlowException fe) {
            String errInfo = fe.getErrorCode() != null ? fe.getErrorCode().toString() : "";
            logger.warn(errInfo, fe);
            fail(fe.getErrorCode());
        } catch (Throwable t) {
            logger.warn(String.format("unhandled exception call backtrace %s", DebugUtils.getStackTrace(t)));
            logger.warn(String.format("[FlowChain(%s): %s] unhandled exception when executing flow[%s], start to rollback",
                    id, name, flow.getClass().getName()), t);
            fail(inerr(t.getMessage()));
        }
    }

    private void rollbackFlow(Flow flow) {
        try {
            printDebugLog(String.format("[FlowChain(%s): %s] start to rollback flow[%s]", id, name, getFlowName(flow)));

            currentLoop --;

            if (contextHandler != null) {
                if (contextHandler.skipRollback(this.getErrorCode())) {
                    this.skipRestRollbacks();
                    this.rollback();
                    return;
                }
            }


            flow.rollback(this, data);
        } catch (Throwable t) {
            logger.warn(String.format("unhandled exception when rollback, call backtrace %s", DebugUtils.getStackTrace(t)));
            logger.warn(String.format("[FlowChain(%s): %s] unhandled exception when rollback flow[%s]," +
                    " continue to next rollback", id, name, flow.getClass().getSimpleName()), t);
            rollback();
        }
    }

    private void callErrorHandler(boolean info) {
        if (info) {
            printDebugLog(String.format("[FlowChain(%s): %s] rolled back all flows because error:\n%s",
                    id, name, errorCode == null ? "" : errorCode.getReadableDetails()));
        }

        if (errorHandler != null) {
            try {
                errorHandler.handle(errorCode, this.data);
            } catch (Throwable t) {
                logger.warn(String.format("unhandled exception when calling %s", errorHandler.getClass()), t);
            }
        }

        if (!afterError.isEmpty()) {
            Collections.reverse(afterError);

            for (List<Runnable> errors : afterError) {
                CollectionUtils.safeForEach(errors, arg -> {
                    if (logger.isTraceEnabled() && enableDebugLog) {
                        logger.trace(String.format("call after error handler %s", arg.getClass()));
                    }
                    arg.run();
                });
            }
        }

        callFinallyHandler();
    }

    private String getFlowNameWithoutLocation(Flow flow) {
        String name = getFlowName(flow);
        if (name.indexOf(" (location:") > 0) {
            return name.substring(0, name.indexOf(" (location:"));
        }
        return name;
    }

    private String getFlowName(Flow flow) {
        StringBuilder name = new StringBuilder();
        name.append(flow.name());

        if (logger.isTraceEnabled()) {
            String className = flow.getClass().getName();
            String[] ff = className.split("\\.");
            String filename = ff[ff.length-1];
            if (filename.contains("$")) {
                int index = filename.indexOf("$");
                filename = filename.substring(0, index);
            }
            name.insert(0, String.format("%s.java: ", filename));
        }
        name.append(String.format(" (location:%d/%d)", currentLoop, flows.size()));

        return name.toString();
    }

    @Override
    public void rollback() {
        if (!isFailCalled) {
            throw new CloudRuntimeException("rollback() cannot be called before fail() is called");
        }

        isRollbackStart = true;
        if (rollBackFlows.empty()) {
            callErrorHandler(true);
            return;
        }

        if (skipRestRollbacks) {
            List<String> restRollbackNames = CollectionUtils.transformAndRemoveNull(rollBackFlows, arg -> arg.getClass().getSimpleName());

            printDebugLog(String.format("[FlowChain(%s): %s] we are instructed to skip rollbacks for remaining flows%s",
                    id, name, restRollbackNames));
            callErrorHandler(true);
            return;
        }

        if (currentRollbackFlow != null) {
            printDebugLog(String.format("[FlowChain(%s): %s] successfully rolled back flow[%s]",
                    id, name, getFlowName(currentRollbackFlow)));
        } else {
            printDebugLog(String.format("[FlowChain(%s): %s] start to rollback", id, name));
        }

        Flow flow = rollBackFlows.pop();
        currentRollbackFlow = flow;
        rollbackFlow(flow);
    }

    @Override
    public void skipRestRollbacks() {
        skipRestRollbacks = true;
    }

    @Override
    public ErrorCode getErrorCode() {
        return errorCode;
    }

    @Override
    public void setError(ErrorCode error) {
        setErrorCode(error);
    }

    private void callFinallyHandler() {
        if (finallyHandler != null) {
            try {
                finallyHandler.Finally();
            } catch (Throwable t) {
                logger.warn(String.format("unhandled exception when calling %s", finallyHandler.getClass()), t);
            }
        }

        if (!afterFinal.isEmpty()) {
            Collections.reverse(afterFinal);

            for (List<Runnable> finals : afterFinal) {
                CollectionUtils.safeForEach(finals, arg -> {
                    if (logger.isTraceEnabled() && enableDebugLog) {
                        logger.trace(String.format("call after final handler %s", arg.getClass()));
                    }

                    arg.run();
                });
            }
        }
    }

    private void callDoneHandler() {
        if (CoreGlobalProperty.PROFILER_WORKFLOW || allowWatch) {
            stopWatch.stop();
        }

        if (doneHandler != null) {
            try {
                doneHandler.handle(this.data);
            } catch (Throwable t) {
                logger.warn(String.format("unhandled exception when calling %s", doneHandler.getClass()), t);
            }
        }

        printDebugLog(String.format("[FlowChain(%s): %s] successfully completed", id, name));

        if (!afterDone.isEmpty()) {
            Collections.reverse(afterDone);

            for (List<Runnable> dones : afterDone) {
                CollectionUtils.safeForEach(dones, arg -> {
                    if (logger.isTraceEnabled() && enableDebugLog) {
                        logger.trace(String.format("call after done handler %s", arg.getClass()));
                    }
                    arg.run();
                });
            }
        }

        callFinallyHandler();
    }

    @Override
    public void fail(ErrorCode errorCode) {
        isFailCalled = true;
        setErrorCode(errorCode);
        rollBackFlows.push(currentFlow);

        if (processors != null) {
            for (FlowChainProcessor p : processors) {
                p.beforeChainRollback(this, currentLoop);
            }
        }

        rollback();
    }

    private boolean isSkipFlow(Flow flow) {
        boolean skip = flow.skip(data);
        if (!skip && contextHandler != null) {
            skip = contextHandler.skipFlow(flow);
        }
        return skip;
    }

    private void runFlowOrComplete() {
        if (it.hasNext()) {
            currentLoop ++;
            runFlow(it.next());
        } else {
            if (getErrorCode() == null) {
                callDoneHandler();
            } else {
                callErrorHandler(false);
            }
        }
    }

    @Override
    public void next() {
        if (!isStart) {
            throw new CloudRuntimeException(
                    String.format("[FlowChain(%s): %s] you must call start() first, and only call next() in Flow.run()",
                            id, name));
        }

        if (isRollbackStart) {
            throw new CloudRuntimeException(
                    String.format("[FlowChain(%s): %s] rollback has started, you can't call next()", id, name));
        }

        rollBackFlows.push(currentFlow);

        if (processors != null) {
            for (FlowChainProcessor p : processors) {
                p.afterOneFlowDone(this, currentLoop);
            }
        }

        printDebugLog(String.format("[FlowChain(%s): %s] successfully executed flow[%s]",
                id, name, getFlowName(currentFlow)));

        runFlowOrComplete();
    }

    @Override
    public void start() {
        if (processors != null) {
            for (FlowChainProcessor p : processors) {
                p.beforeChainStart(this);
            }
        }

        if (flows.isEmpty() && allowEmptyFlow) {
            callDoneHandler();
            return;
        }

        if (flows.isEmpty()) {
            throw new CloudRuntimeException("you must call then() to add flow before calling start() or allowEmptyFlow() to run empty flow chain on purpose");
        }

        if (data == null) {
            data = new HashMap<>();
        }

        isStart = true;
        if (name == null) {
            name = "anonymous-chain";
        }

        printDebugLog(String.format("[FlowChain(%s): %s] starts", id, name));

        if (logger.isTraceEnabled() && enableDebugLog) {
            List<String> names = CollectionUtils.transformAndRemoveNull(flows, arg -> String.format("%s[%s]", arg.getClass(), getFlowNameWithoutLocation(arg)));
            logger.trace(String.format("execution path:\n%s", StringUtils.join(names, " -->\n")));
        }

        it = flows.iterator();
        runFlowOrComplete();
    }

    @Override
    public FlowChain noRollback(boolean no) {
        skipRestRollbacks = no;
        return this;
    }

    @Override
    public FlowChain allowEmptyFlow() {
        allowEmptyFlow = true;
        return this;
    }

    private void setErrorCode(ErrorCode errorCode) {
        this.errorCode = errorCode;
        if (errorCode != null) {
            this.errorCode.setLocation(getFlowName(currentFlow));
        }
    }

    public static Map<String, WorkFlowStatistic> getStatistics() {
        return statistics;
    }
}
