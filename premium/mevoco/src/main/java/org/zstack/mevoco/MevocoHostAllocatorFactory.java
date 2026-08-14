package org.zstack.mevoco;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.allocator.HostSortorChain;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.header.Component;
import org.zstack.header.allocator.*;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by frank on 9/26/2015.
 */
public class MevocoHostAllocatorFactory implements HostAllocatorStrategyFactory, Component {
    private static final CLogger logger = Utils.getLogger(MevocoHostAllocatorFactory.class);
    private static final HostAllocatorStrategyType type = new HostAllocatorStrategyType(MevocoConstants.MEVOCO_HOST_ALLOCATOR_STRATEGY);

    private Map<String, HostAllocatorStrategyFactory> factories = Collections.synchronizedMap(new HashMap<String, HostAllocatorStrategyFactory>());

    @Autowired
    private PluginRegistry pluginRgty;

    @Override
    public HostAllocatorStrategyType getHostAllocatorStrategyType() {
        return type;
    }

    @Override
    public HostAllocatorStrategy getHostAllocatorStrategy() {
        String strategyName = MevocoGlobalConfig.HOST_ALLOCATOR_STRATEGY.value();
        HostAllocatorStrategyFactory f = factories.get(strategyName);
        if (f == null) {
            throw new CloudRuntimeException(String.format("cannot find HostAllocatorStrategy with type[%s]", strategyName));
        }

        logger.debug(String.format("mevoco chooses HostAllocatorStrategy[type:%s]", f.getHostAllocatorStrategyType()));
        return f.getHostAllocatorStrategy();
    }

    @Override
    public void marshalSpec(HostAllocatorSpec spec, AllocateHostMsg msg) {
    }

    @Override
    public boolean start() {
        for (HostAllocatorStrategyFactory ext : pluginRgty.getExtensionList(HostAllocatorStrategyFactory.class)) {
            if (ext.getHostAllocatorStrategyType().equals(type)) {
                continue;
            }

            HostAllocatorStrategyFactory old = factories.get(ext.getHostAllocatorStrategyType().toString());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate HostAllocatorStrategyFactory[%s, %s] for type[%s]",
                        old.getClass().getName(), ext.getClass().getName(), ext.getHostAllocatorStrategy()));
            }
            factories.put(ext.getHostAllocatorStrategyType().toString(), ext);
        }
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public HostSortorStrategy getHostSortorStrategy() {
        return new HostSortorChain();
    }
}
