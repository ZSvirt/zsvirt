package org.zstack.header.core.encrypt;

import org.zstack.header.message.NeedReplyMessage;

/**
 * @author hanyu.liang
 * @date 2023/4/17 16:33
 */
public class RemoveIntegrityVerificationFileMsg  extends NeedReplyMessage {
    private String nodeType;
    private String nodeUuid;
    private String path;

    public String getNodeType() {
        return nodeType;
    }

    public void setNodeType(String nodeType) {
        this.nodeType = nodeType;
    }

    public String getNodeUuid() {
        return nodeUuid;
    }

    public void setNodeUuid(String nodeUuid) {
        this.nodeUuid = nodeUuid;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}

