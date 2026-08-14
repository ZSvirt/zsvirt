package org.zstack.core.componentloader;

import org.springframework.beans.factory.BeanFactory;

public interface ComponentLoader {
    <T> T getComponent(Class<T> clazz);

    boolean hasComponent(Class clazz);

    <T> T getComponentNoExceptionWhenNotExisting(Class<T> clazz);

    <T> T getComponent(String className);
    
    <T> T getComponentByBeanName(String beanName);
    
    PluginRegistry getPluginRegistry();

    BeanFactory getSpringIoc();
}
