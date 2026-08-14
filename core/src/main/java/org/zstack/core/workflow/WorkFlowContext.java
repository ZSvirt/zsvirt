package org.zstack.core.workflow;

import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.serializable.SerializableHelper;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class WorkFlowContext implements Serializable {
    private Map<String, Object> context = new HashMap<String, Object>();
    
    public void put(String key, Object value) {
        if (!(value instanceof Serializable)) {
        	throw new IllegalArgumentException(String.format("value[%s] must be Serializable", value.getClass().getName()));
        }
        context.put(key, value);
    }
    
    public Object get(String key) {
        return context.get(key);
    }
    
    public void remove(String key) {
        context.remove(key);
    }
    
    static WorkFlowContext fromBytes(byte[] bytes) {
        try {
            return SerializableHelper.readObject(bytes);
        } catch (Exception e) {
            throw new CloudRuntimeException("Unable to create WorkFlowContext from input bytes", e);
        }
    }
    
    byte[] toBytes() {
        try {
            return SerializableHelper.writeObject(this);
        } catch (IOException e) {
            throw new CloudRuntimeException("Cannot write WorkFlowContext to bytes", e);
        }
    }
}
