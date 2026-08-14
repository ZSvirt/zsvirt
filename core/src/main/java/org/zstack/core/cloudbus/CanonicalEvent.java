package org.zstack.core.cloudbus;

import org.zstack.header.log.NoLogging;
import org.zstack.header.message.LocalEvent;
import org.zstack.utils.JsonWrapper;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 11:18 PM
 * To change this template use File | Settings | File Templates.
 */
public class CanonicalEvent extends LocalEvent {
    private String path;
    @NoLogging(behavior = NoLogging.Behavior.Auto)
    private Object content;
    private String managementNodeId;

    public Map toStructForWebhook() {
        Map m = new LinkedHashMap();
        m.put("path", path);
        m.put("content", content);
        m.put("managementNodeId", managementNodeId);
        return m;
    }

    public String getManagementNodeId() {
        return managementNodeId;
    }

    public void setManagementNodeId(String managementNodeId) {
        this.managementNodeId = managementNodeId;
    }

    @Override
    public String getSubCategory() {
        return "canonicalEvent";
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Object getContent() {
        return content;
    }

    public void setContent(Object content) {
        this.content = content;
    }
}
