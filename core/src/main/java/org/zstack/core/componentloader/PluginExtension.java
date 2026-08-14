package org.zstack.core.componentloader;

import java.util.HashMap;
import java.util.Map;

public class PluginExtension {
	private String beanClassName;
	private String beanName;
    private String instanceId;
    private String referenceInterface;
    private Object instance;
    private int order;
    private Map<String, String> attributes = new HashMap<String, String>();
    
    PluginExtension() {
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public Object getInstance() {
        return instance;
    }

    public void setInstance(Object instance) {
        this.instance = instance;
    }

    
    public String getReferenceInterface() {
    	return referenceInterface;
    }

	public void setReferenceInterface(String referenceInterface) {
    	this.referenceInterface = referenceInterface;
    }

	public String getBeanClassName() {
    	return beanClassName;
    }

	public void setBeanClassName(String beanClassName) {
    	this.beanClassName = beanClassName;
    }

	@Override
    public String toString() {
        return getReferenceInterface();
    }

    public String getBeanName() {
        return beanName;
    }

    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

	public int getOrder() {
		return order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

    public String getAttribute(String key) {
        return attributes.get(key);
    }

    public void putAttribute(String key, String val) {
        attributes.put(key, val);
    }
}
