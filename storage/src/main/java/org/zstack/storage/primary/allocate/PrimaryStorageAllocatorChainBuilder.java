package org.zstack.storage.primary.allocate;

import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.storage.primary.allocate.AbstractPrimaryStorageAllocatorFlow;

import java.util.ArrayList;
import java.util.List;

public class PrimaryStorageAllocatorChainBuilder {
    private List<String> producerClassNames;
    private List<String> flowClassNames;
    private boolean isConstructed;
    private List<Class<?>> producerClasses = new ArrayList<>();
    private List<Class<?>> flowClasses = new ArrayList<>();

    public static PrimaryStorageAllocatorChainBuilder newBuilder() {
        return new PrimaryStorageAllocatorChainBuilder();
    }

    public PrimaryStorageAllocatorChainBuilder construct() {
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
            isConstructed = true;
            return this;
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
    }

    public PrimaryStorageAllocatorChain build() {
        if (!isConstructed) {
            construct();
        }

        List<AbstractPrimaryStorageAllocatorFlow> flows = new ArrayList<>();
        try {
            for (Class<?> producerClass : producerClasses) {
                flows.add((AbstractPrimaryStorageAllocatorFlow) producerClass.newInstance());
            }
            for (Class<?> flowClass : flowClasses) {
                flows.add((AbstractPrimaryStorageAllocatorFlow) flowClass.newInstance());
            }
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }

        PrimaryStorageAllocatorChain chain = new PrimaryStorageAllocatorChain();
        chain.setFlows(flows);
        return chain;
    }

    public PrimaryStorageAllocatorChainBuilder setProducerClassNames(List<String> producerClassNames) {
        this.producerClassNames = producerClassNames;
        return this;
    }

    public PrimaryStorageAllocatorChainBuilder setFlowClassNames(List<String> flowClassNames) {
        this.flowClassNames = flowClassNames;
        return this;
    }
}
