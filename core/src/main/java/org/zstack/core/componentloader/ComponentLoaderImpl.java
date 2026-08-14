package org.zstack.core.componentloader;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.logging.CLoggerImpl;


public class ComponentLoaderImpl implements ComponentLoader {
    private static final CLogger logger = CLoggerImpl.getLogger(ComponentLoaderImpl.class);
    private final BeanFactory  ioc;
    private PluginRegistryIN pluginRegistry = null;
    private static boolean isInit = false;


    private void checkInit() {
        if (isInit) {
            throw new CloudRuntimeException("Nested ComponentLoader initialization detected. DO NOT call Platform.getComponentLoader() in bean's constructor, it causes nested initialization");
        }
        isInit = true;
    }

    public ComponentLoaderImpl(ApplicationContext appContext) {
        checkInit();
        ioc = appContext;
    }


    public ComponentLoaderImpl () {
        checkInit();

        ioc = new ClassPathXmlApplicationContext(String.format("classpath:%s", CoreGlobalProperty.BEAN_CONF));
    }

    @Override
    public <T> T getComponent(Class<T> clazz) {
        return ioc.getBean(clazz);
    }

    @Override
    public boolean hasComponent(Class clazz) {
        try {
            getComponent(clazz);
            return true;
        } catch (NoSuchBeanDefinitionException e) {
            return false;
        }
    }

    @Override
    public <T> T getComponentNoExceptionWhenNotExisting(Class<T> clazz) {
        try {
            return getComponent(clazz);
        } catch (NoSuchBeanDefinitionException ne) {
            return null;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getComponent(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            return (T) getComponent(clazz);
        } catch (ClassNotFoundException e) {
            String err = "Cannot find bean with class " + className;
            throw new CloudRuntimeException(err, e);
        }
    }

    @Override
    public PluginRegistry getPluginRegistry() {
        /*
         *  pluginRegistry must be constructed here, not in constructor of ComponentLoaderImpl.
         *  PluginRegistry which requires ComponentLoader in its initialize() is a Spring bean that
         *  is constructed when creating Spring ApplicationContext. Given ApplicationContext is created in
         *  constructor of ComponentLoaderImpl, constructing PluginRegistry in the same place either results
         *  two ApplicationContext or a circle constructing loop.
         */
        if (pluginRegistry == null) {
            try {
                pluginRegistry = ioc.getBean(PluginRegistryIN.class);
            } catch (NoSuchBeanDefinitionException e) {
                // unittest which loads simple bean may not lead PluginRegistry being created,
                // catch the exception and ignore it.
                logger.debug("if you are running unit test, this exception is safe. The reason is beans you load in unittest don't have zstack plugin declaration, so PluginRegistry won't create", e);
                return null;
            }
            pluginRegistry.initialize();
        }
        return pluginRegistry;
    }

    @Override
    public BeanFactory getSpringIoc() {
        return ioc;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getComponentByBeanName(String beanName) {
        return (T) ioc.getBean(beanName);
    }
}
