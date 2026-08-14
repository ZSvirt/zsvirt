package org.zstack.compute.allocator;

import org.zstack.header.allocator.AbstractHostAllocatorFlow;
import org.zstack.header.allocator.AbstractHostSortorFlow;
import org.zstack.header.allocator.HostCandidateProducer;
import org.zstack.header.exception.CloudRuntimeException;

import java.util.ArrayList;
import java.util.List;

/**
 */
public class HostAllocatorChainBuilder {
    private List<String> producerClassNames;
    private List<String> flowClassNames;
    private List<String> sorterClassNames;
    private boolean isConstructed;
    private List<Class<?>> producerClasses = new ArrayList<>();
    private List<Class<?>> flowClasses = new ArrayList<>();
    private List<Class<?>> sorterClasses = new ArrayList<>();

    public static HostAllocatorChain newAllocationChain() {
        return new HostAllocatorChain();
    }

    public static HostSortorChain newSortChain() {
        return new HostSortorChain();
    }

    public static HostAllocatorChainBuilder newBuilder() {
        return new HostAllocatorChainBuilder();
    }

    public HostAllocatorChainBuilder construct() {
        try {
            if (producerClassNames != null) {
                for (String clzName : producerClassNames) {
                    producerClasses.add(Class.forName(clzName));
                }
            }
            if (flowClassNames != null) {
                for (String clzName : flowClassNames) {
                    flowClasses.add(Class.forName(clzName));
                }
            }
            if (sorterClassNames != null) {
                for (String clzName : sorterClassNames) {
                    sorterClasses.add(Class.forName(clzName));
                }
            }

            isConstructed = true;
            return this;
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
    }

    private List<HostCandidateProducer> buildProducers() {
        List<HostCandidateProducer> list = new ArrayList<>();
        try {
            for (Class<?> clazz : producerClasses) {
                list.add((HostCandidateProducer) clazz.newInstance());
            }
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
        return list;
    }

    private List<AbstractHostAllocatorFlow> buildFlows() {
        List<AbstractHostAllocatorFlow> flows = new ArrayList<>();
        try {
            for (Class<?> flowClass : flowClasses) {
                flows.add((AbstractHostAllocatorFlow) flowClass.newInstance());
            }
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
        return flows;
    }

    private List<AbstractHostSortorFlow> buildSortFlows() {
        List<AbstractHostSortorFlow> flows = new ArrayList<>();
        try {
            flows.add(RandomSortFlow.class.newInstance());
            for (Class<?> flowClass : sorterClasses) {
                flows.add((AbstractHostSortorFlow) flowClass.newInstance());
            }
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
        return flows;
    }

    public HostAllocatorChain build() {
        if (!isConstructed) {
            construct();
        }

        HostAllocatorChain chain = newAllocationChain();
        chain.setProducers(buildProducers());
        chain.setFlows(buildFlows());
        return chain;
    }

    public HostSortorChain buildSort() {
        if (!isConstructed) {
            construct();
        }

        HostSortorChain chain = newSortChain();
        chain.setFlows(buildSortFlows());
        return chain;
    }

    public HostAllocatorChainBuilder setProducerClassNames(List<String> producerClassNames) {
        this.producerClassNames = producerClassNames;
        return this;
    }

    public HostAllocatorChainBuilder setFlowClassNames(List<String> flowClassNames) {
        this.flowClassNames = flowClassNames;
        return this;
    }

    public HostAllocatorChainBuilder setSorterClassNames(List<String> sorterClassNames) {
        this.sorterClassNames = sorterClassNames;
        return this;
    }
}
