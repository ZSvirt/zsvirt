package org.zstack.storage.backup.imagestore;

import org.zstack.header.message.MessageReply;

import java.util.List;

public class ExportImageNbdChainReply extends MessageReply {
    private String hostname;
    private String sessionId;
    private int port;
    private List<String> exportNames;

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public List<String> getExportNames() {
        return exportNames;
    }

    public void setExportNames(List<String> exportNames) {
        this.exportNames = exportNames;
    }
}
