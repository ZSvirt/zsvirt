package org.zstack.portal.managementnode;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.web.context.ContextLoaderListener;
import org.zstack.core.CoreGlobalProperty;

import javax.servlet.ServletContext;

public class BootstrapContextLoaderListener extends ContextLoaderListener {
    private static ApplicationContext parent;

    @Override
    protected ApplicationContext loadParentContext(ServletContext servletContext) {
        if (parent == null) {
            String configLocation = String.format("classpath:%s", CoreGlobalProperty.BEAN_CONF);
            parent = new ClassPathXmlApplicationContext(configLocation);
        }

        return parent;
    }
}
