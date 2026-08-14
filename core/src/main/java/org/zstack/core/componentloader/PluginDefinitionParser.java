package org.zstack.core.componentloader;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.BeanDefinitionDecorator;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PluginDefinitionParser implements BeanDefinitionDecorator {
    private static final String EXTENSION_NODE = "zstack:extension";
    private static final String PLUGIN_NODE = "zstack:plugin";
    private static final CLogger logger = Utils.getLogger(PluginDefinitionParser.class);

    private List<PluginExtension> parsePlugin(List<Element> all, BeanDefinition bean, String beanName) {
        List<PluginExtension> exts = new ArrayList<>(all.size() - 1);
        boolean find = false;
        for (Element e : all) {
            if (e.getTagName().equals(PLUGIN_NODE)) {
                find = true;
            }
            if (e.getTagName().equals(EXTENSION_NODE)) {
                assert find;
                PluginExtension ext = new PluginExtension();
                ext.setBeanClassName(bean.getBeanClassName());
                ext.setBeanName(beanName);
                String iface = e.getAttribute("interface");
                ext.setReferenceInterface(iface.trim());
                String iref = e.getAttribute("instance-ref");
                ext.setInstanceId(iref.trim());
                String order = e.getAttribute("order");
                order = order.trim();
                if (!order.equals("")) {
                    ext.setOrder(Integer.parseInt(order));
                }

                NamedNodeMap attrMap = e.getAttributes();
                for (int i=0; i<attrMap.getLength(); i++) {
                    Attr attr = (Attr) attrMap.item(i);
                    ext.putAttribute(attr.getNodeName(), attr.getNodeValue());
                }
                exts.add(ext);
            }
        }

        return exts;
    }

    @Override
    public BeanDefinitionHolder decorate(Node node, BeanDefinitionHolder holder, ParserContext ctx) {
        if (!ctx.getRegistry().containsBeanDefinition(PluginRegistry.PLUGIN_REGISTRY_BEAN_NAME)) {
            BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(PluginRegistryImpl.class);
            ctx.getRegistry().registerBeanDefinition(PluginRegistry.PLUGIN_REGISTRY_BEAN_NAME, builder.getBeanDefinition());
            logger.debug("No BeanDefinition for PluginRegistry found, create and register one");
        }
        
        Element root = (Element) node;
        List<Element> all = new ArrayList<Element>(6);
        all.add(root);
        List<Element> children = DomUtils.getChildElementsByTagName(root, new String[] { EXTENSION_NODE });
        all.addAll(children);

        List<PluginExtension> exts = parsePlugin(all, holder.getBeanDefinition(), holder.getBeanName());
        if (!exts.isEmpty()) {
            PluginExtension ext = exts.get(0);
            BeanDefinition registryBean = ctx.getRegistry().getBeanDefinition(PluginRegistry.PLUGIN_REGISTRY_BEAN_NAME);
            MutablePropertyValues props = registryBean.getPropertyValues();
            PropertyValue prop = props.getPropertyValue(PluginRegistry.PLUGIN_REGISTRYIMPL_PLUGINS_FIELD_NAME);
            if (prop == null) {
                Map<String, List<PluginExtension>> extensions = new HashMap<String, List<PluginExtension>>(1);
                extensions.put(ext.getBeanClassName(), exts);
                prop = new PropertyValue(PluginRegistry.PLUGIN_REGISTRYIMPL_PLUGINS_FIELD_NAME, extensions);
                if (logger.isTraceEnabled()) {
                    logger.trace("No 'extensions' property found in PluginRegistry bean definition, create a new one");
                }
                props.addPropertyValue(prop);
            } else {
                Map<String, List<PluginExtension>> extensions = (Map<String, List<PluginExtension>>) prop.getValue();
                List<PluginExtension> oexts = extensions.computeIfAbsent(ext.getBeanClassName(), k -> new ArrayList<>(exts.size()));

                for (PluginExtension e : exts) {
                    if (oexts.contains(e)) {
                        throw new CloudRuntimeException(String.format("duplicated extension declaration[interfaceRef:%s, beanName:%s, beanClass:%s]", e.getReferenceInterface(), ext.getBeanName(), ext.getBeanClassName()));
                    }
                }

                oexts.addAll(exts);
            }

            if (logger.isTraceEnabled()) {
                logger.trace(String.format("Add extensions declared by bean[name=%s, class=%s] to 'extensions' property of PluginRegistry bean definition", ext.getBeanName(), ext.getBeanClassName()));
            }
        }

        return holder;
    }
}
