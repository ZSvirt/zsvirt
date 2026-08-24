package org.zstack.header.message;

import org.zstack.header.Constants;
import org.zstack.header.core.AsyncBackup;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.rest.APINoSee;
import org.zstack.utils.StringDSL;
import org.zstack.utils.string.Uuid;

import java.io.Serializable;
import java.util.*;

import static org.zstack.utils.BeanUtils.getProperty;
import static org.zstack.utils.BeanUtils.setProperty;
import static org.zstack.utils.gson.JSONObjectUtil.rehashObject;


public abstract class Message implements Serializable, AsyncBackup, Cloneable {
    @APINoSee
    @NoJsonSchema
    private Map<String, Object> headers = new LinkedHashMap<>();
    @APINoSee
    private String id;
    @APINoSee
    private String serviceId;
    @APINoSee
    private long createdTime;

    protected static String uuid() {
        String uuidForExample = System.getProperty(Constants.UUID_FOR_EXAMPLE);
        if(uuidForExample != null){
            return DocUtils.uuidForAPIDoc();
        }else{
            return Uuid.generate();
        }
    }

    /**
     * return a fixed uuid that starts with "ff0" and ends with "00"
     */
    protected static String uuid(Class<?> voClass) {
        return StringDSL.createFixedUuid(voClass);
    }

    public Message() {
        createdTime = System.currentTimeMillis();
        id = Uuid.generate();
    }

    public Map<String, Object> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, Object> headers) {
        this.headers = headers;
    }

    public void putHeaderEntry(String key, Object value) {
        headers.put(key, value);
    }

    public <T> T getHeaderEntry(String key) {
        return (T) headers.get(key);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public long getCreatedTime() {
        return createdTime;
    }

    public String getMessageName() {
        return this.getClass().getCanonicalName();
    }

    @Override
    public boolean equals(Object t) {
        if (t == null || !(t instanceof Message)) {
            return false;
        }

        return ((Message) t).getId().equals(getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    @Override
    public Message clone() {
        try {
            return (Message) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new CloudRuntimeException(e);
        }
    }

    public void setCreatedTime(long createdTime) {
        this.createdTime = createdTime;
    }

    public void restoreFromSchema(Map raw) throws ClassNotFoundException {
        Map<String, String> schema = this.getHeaderEntry("schema");
        if (schema == null || schema.isEmpty()) {
            return;
        }

        List<String> paths = new ArrayList<>(schema.keySet());

        for (String p : paths) {
            Object dst = getProperty(this, p);
            String type = schema.get(p);

            if (dst.getClass().getName().equals(type)) {
                continue;
            }

            Class clz = Class.forName(type);
            setProperty(this, p, rehashObject(getProperty(raw, p), clz));
        }
    }
}