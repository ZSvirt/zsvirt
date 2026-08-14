package org.zstack.header.console;

import java.net.URI;

public class ConsoleUrl {
    private URI uri;
    private String version;
    private boolean needConsoleProxy = true;

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public boolean isNeedConsoleProxy() {
        return needConsoleProxy;
    }

    public void setNeedConsoleProxy(boolean needConsoleProxy) {
        this.needConsoleProxy = needConsoleProxy;
    }
}
