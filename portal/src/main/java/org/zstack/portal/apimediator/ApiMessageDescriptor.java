package org.zstack.portal.apimediator;

import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.message.APIParam;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 11:34 PM
 * To change this template use File | Settings | File Templates.
 */
public class ApiMessageDescriptor {
    private String name;
    private String serviceId;
    private String configPath;
    private List<String> roles;
    private List<ApiMessageInterceptor> interceptors;
    private Class<?> clazz;
    private Map<Field, APIParam> fieldApiParams = new HashMap<Field, APIParam>();

    public Map<Field, APIParam> getFieldApiParams() {
        return fieldApiParams;
    }

    public void setFieldApiParams(Map<Field, APIParam> fieldApiParams) {
        this.fieldApiParams = fieldApiParams;
    }

    private Map<String, APIParam> overriddenApiParams = new HashMap<String, APIParam>();

    public Map<String, APIParam> getOverriddenApiParams() {
        return overriddenApiParams;
    }

    public void setOverriddenApiParams(Map<String, APIParam> overriddenApiParams) {
        this.overriddenApiParams = overriddenApiParams;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public List<ApiMessageInterceptor> getInterceptors() {
        return interceptors;
    }

    public void setInterceptors(List<ApiMessageInterceptor> interceptors) {
        this.interceptors = interceptors;
    }

    public String getConfigPath() {
        return configPath;
    }

    public void setConfigPath(String configPath) {
        this.configPath = configPath;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public Class<?> getClazz() {
        return clazz;
    }

    public void setClazz(Class<?> clazz) {
        this.clazz = clazz;
    }
}
