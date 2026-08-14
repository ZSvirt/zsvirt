package org.zstack.header.storage.backup;

import org.apache.commons.lang.StringUtils;
import org.zstack.header.exception.CloudRuntimeException;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * @ Author : yh.w
 * @ Date   : Created in 16:56 2023/10/11
 */
public class NbdRemoteTarget implements RemoteTarget {
    private String host;
    private int port;
    private String exportName;
    private String options;

    public NbdRemoteTarget(String host, int port, String device, String options) {
        this.host = host;
        this.port = port;
        this.exportName = device;
        this.options = options;
    }

    public NbdRemoteTarget(String nbdUri) {
        URI uri;
        try {
            uri = new URI(nbdUri);
        } catch (URISyntaxException e) {
            throw new CloudRuntimeException("NBD URI syntax is incorrect: " + nbdUri, e);
        }

        this.host = uri.getHost();
        this.port = uri.getPort();
        this.exportName = StringUtils.isEmpty(uri.getPath()) ? null : uri.getPath().substring(1);
        this.options  = uri.getQuery();
    }

    @Override
    public String getInstallPath() {
        return null;
    }

    @Override
    public String getTargetUri() {
        return getTargetUri(this.host);
    }

    @Override
    public String getTargetUri(String hostname) {
        StringBuilder uriBuilder = new StringBuilder();
        uriBuilder.append("nbd://");
        uriBuilder.append(hostname);
        uriBuilder.append(":").append(this.port);
        if (!StringUtils.isEmpty(this.exportName)) {
            uriBuilder.append("/").append(this.getExportName());
        }

        if (!StringUtils.isEmpty(this.options)) {
            uriBuilder.append("?").append(this.options);
        }
        return uriBuilder.toString();
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getExportName() {
        return exportName;
    }

    public void setExportName(String exportName) {
        this.exportName = exportName;
    }

    public String getOptions() {
        return options;
    }

    public void setOptions(String options) {
        this.options = options;
    }
}
