package org.zstack.abstraction.entity.sns;

import java.util.Map;

/**
 * PluginEndpointData used to copy sns message data for plugin.
 */
public class PluginEndpointData {
    private Map<String, Object> metadata;
    private String message;
    private Map<String, Object> properties;

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

}
