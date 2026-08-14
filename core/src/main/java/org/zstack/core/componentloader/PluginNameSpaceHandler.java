package org.zstack.core.componentloader;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

public class PluginNameSpaceHandler extends NamespaceHandlerSupport {

    @Override
    public void init() {
        this.registerBeanDefinitionDecorator("plugin", new PluginDefinitionParser());
    }

}
