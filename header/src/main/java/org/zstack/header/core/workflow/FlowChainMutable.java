package org.zstack.header.core.workflow;

import java.util.List;
import java.util.Map;

/**
 * Created by xing5 on 2016/4/4.
 */
public interface FlowChainMutable {
    List<Flow> getFlows();

    void setFlows(List<Flow> flows);

    FlowDoneHandler getFlowDoneHandler();

    void setFlowDoneHandler(FlowDoneHandler handler);

    FlowErrorHandler getFlowErrorHandler();

    void setFlowErrorHandler(FlowErrorHandler handler);

    FlowFinallyHandler getFlowFinallyHandler();

    void setFlowFinallyHandler(FlowFinallyHandler handler);

    String getChainName();

    void setChainName(String name);

    Map getChainData();

    void setChainData(Map data);
}
