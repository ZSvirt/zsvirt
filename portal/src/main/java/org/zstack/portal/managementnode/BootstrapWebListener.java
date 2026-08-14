package org.zstack.portal.managementnode;

import org.zstack.core.Platform;
import org.zstack.utils.BootErrorLog;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 */
public class BootstrapWebListener implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        try {
            // this make sure Platform's static block executes before spring initialization
            Platform.getUuid();
        } catch (RuntimeException e) {
            new BootErrorLog().write(e.getMessage());
            throw e;
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
    }
}
