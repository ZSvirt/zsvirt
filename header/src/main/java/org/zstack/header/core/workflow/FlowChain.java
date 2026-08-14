package org.zstack.header.core.workflow;

import org.zstack.header.errorcode.ErrorCode;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 */
public interface FlowChain {
    List<Flow> getFlows();

    FlowChain insert(Flow flow);

    FlowChain insert(int pos, Flow flow);

    FlowChain setFlowMarshaller(FlowMarshaller marshaller);

    FlowChain then(Flow flow);

    FlowChain done(FlowDoneHandler handler);

    FlowChain error(FlowErrorHandler handler);

    FlowChain ctxHandler(FlowContextHandler handler);

    FlowChain Finally(FlowFinallyHandler handler);

    FlowChain setData(Map data);

    FlowChain putData(Map.Entry... es);

    FlowChain setName(String name);

    FlowChain preCheck(Function<Map, ErrorCode> checker);

    void addProcessor(FlowChainProcessor processor);

    Map getData();

    void start();

    FlowChain noRollback(boolean no);

    FlowChain allowEmptyFlow();

    void allowWatch();

    void disableDebugLog();

    FlowChain enableProgressReport();
}
