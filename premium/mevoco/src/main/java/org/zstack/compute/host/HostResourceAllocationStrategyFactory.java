package org.zstack.compute.host;

import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class HostResourceAllocationStrategyFactory {
    protected static final CLogger logger = Utils.getLogger(HostResourceAllocationStrategyFactory.class);

    public HostResourceAllocationStrategyFactory() {}

    public static HostResourceAllocationStrategy getInstance(String strategyName) throws ClassNotFoundException {
        String className = String.format("org.zstack.compute.host.HostResource%sAllocationStrategy", strategyName);
        HostResourceAllocationStrategy strategy = new HostResourceAllocationStrategy();
        try {
            strategy = (HostResourceAllocationStrategy) Class.forName(className).newInstance();
        } catch (ClassNotFoundException e) {
            logger.error(String.format("Not found Strategy[%s], Check strategy that you request, detail: %s", strategyName, e.toString()));
            throw e;
        } catch (InstantiationException e) {
            logger.error(String.format("Init Strategy[%s], Check strategy that you request, detail: %s", strategyName, e.toString()));
        } catch (IllegalAccessException e) {
            logger.error(String.format("Cannot access Strategy[%s], Check strategy that you request, detail: %s", strategyName, e.toString()));
        }
        return strategy;
    }
}
