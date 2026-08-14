package org.zstack.core.workflow;

import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowChain;
import org.zstack.header.exception.CloudRuntimeException;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 8:12 PM
 * To change this template use File | Settings | File Templates.
 */
public class FlowChainBuilder {
    private List<String> flowClassNames;
    private List<Flow> flows = new ArrayList<>();
    private boolean isConstructed;

    public FlowChainBuilder construct() {
        try {
            if (flowClassNames != null) {
                for (Object name : flowClassNames) {
                    String className = (String) name;
                    Class<Flow> clazz = (Class<Flow>) Class.forName(className);
                    Flow flow = clazz.newInstance();
                    flows.add(flow);
                }
            }
            isConstructed = true;
            return this;
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
    }

    public FlowChainBuilder setFlowClassNames(List<String> flowClassNames) {
        this.flowClassNames = flowClassNames;
        return this;
    }

    public FlowChain build(String name) {
        FlowChain chain = build();
        chain.setName(name);
        return chain;
    }

    public FlowChain build() {
        if (!isConstructed) {
            throw new CloudRuntimeException(String.format("please call construct() before build()"));
        }

        SimpleFlowChain chain = new SimpleFlowChain();
        for (Flow flow : flows) {
            chain.then(flow);
        }
        return chain;
    }

    public static FlowChainBuilder newBuilder() {
        return new FlowChainBuilder();
    }

    public static FlowChain newSimpleFlowChain() {
        return new SimpleFlowChain();
    }

    public static FlowChain newShareFlowChain() {
        return new ShareFlowChain();
    }

    public List<Flow> getFlows() {
        return flows;
    }

    public void setFlows(List<Flow> flows) {
        this.flows = flows;
    }
}
