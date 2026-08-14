package org.zstack.core.jmx;

import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;

/**
 */
public class JmxFacadeImpl implements JmxFacade {
    private static final CLogger logger = Utils.getLogger(JmxFacadeImpl.class);

    private MBeanServer mBeanServer;

    private boolean isEnabled() {
        return mBeanServer != null;
    }

    void init() {
        mBeanServer = ManagementFactory.getPlatformMBeanServer();
        if (!isEnabled()) {
            logger.warn(String.format("unable to find a mBeanServer, JMX function will be disabled"));
        }

    }

    @Override
    public void registerBean(String name, Object bean) {
        if (!isEnabled()) {
            logger.warn(String.format("JMX is disabled because no mBeanServer, ignore registering mBean %s", name));
            return;
        }

        try {
            ObjectName mxbeanName = new ObjectName(String.format("org.zstack:name=%s", name));
            mBeanServer.registerMBean(bean, mxbeanName);
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
    }
}
